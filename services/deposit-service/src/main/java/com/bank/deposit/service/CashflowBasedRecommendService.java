package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.entity.ProductInterestRate;
import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.AccountStatus;
import com.bank.deposit.domain.enums.DirectionType;
import com.bank.deposit.domain.enums.ProductStatus;
import com.bank.deposit.domain.enums.ProductType;
import com.bank.deposit.domain.enums.RateType;
import com.bank.deposit.domain.enums.TransactionStatus;
import com.bank.deposit.dto.response.CashFlowSummary;
import com.bank.deposit.dto.response.ProductRecommendResponse;
import com.bank.deposit.dto.response.RecommendedProduct;
import com.bank.deposit.repository.AccountRepository;
import com.bank.deposit.repository.ProductInterestRateRepository;
import com.bank.deposit.repository.ProductRepository;
import com.bank.deposit.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashflowBasedRecommendService {

    private static final int CUSTOMER_BIRTH_YEAR = 1990;
    private static final int YOUTH_MAX_AGE = 34;
    private static final int LOW_TRANSACTION_COUNT = 5;
    private static final int HIGH_TRANSACTION_COUNT = 10;
    private static final BigDecimal SAVINGS_GROWTH_WEIGHT = new BigDecimal("1.30");
    private static final BigDecimal FINANCIAL_FIT_MAX = new BigDecimal("40");
    private static final BigDecimal EXPECTED_RETURN_MAX = new BigDecimal("30");
    private static final BigDecimal LIQUIDITY_MAX = new BigDecimal("20");
    private static final BigDecimal BENEFIT_MAX = new BigDecimal("10");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final ProductInterestRateRepository productInterestRateRepository;
    private final Clock clock;

    public ProductRecommendResponse recommend(String customerId, int periodMonth) {
        Assert.isTrue(periodMonth >= 1, "periodMonth must be greater than or equal to 1.");

        List<Account> activeAccounts = accountRepository.findByCustomerId(customerId).stream()
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .toList();
        List<Long> accountIds = activeAccounts.stream()
                .map(Account::getAccountId)
                .toList();

        if (accountIds.isEmpty()) {
            return emptyResult(customerId, periodMonth);
        }

        OffsetDateTime endAt = OffsetDateTime.now(clock);
        OffsetDateTime startAt = endAt.minusMonths(periodMonth);
        List<Transaction> transactions = transactionRepository
                .findByAccountIdInAndTransactionAtBetweenAndStatus(
                        accountIds, startAt, endAt, TransactionStatus.SUCCESS);

        if (transactions.isEmpty()) {
            return emptyResult(customerId, periodMonth);
        }

        BigDecimal totalInflow = sumByDirection(transactions, DirectionType.IN);
        BigDecimal totalOutflow = sumByDirection(transactions, DirectionType.OUT);
        BigDecimal netCashFlow = totalInflow.subtract(totalOutflow);
        BigDecimal monthlySavings = netCashFlow.divide(
                BigDecimal.valueOf(periodMonth), 0, RoundingMode.DOWN);

        CashFlowSummary cashFlow = new CashFlowSummary(totalInflow, totalOutflow, netCashFlow, monthlySavings);

        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            return new ProductRecommendResponse(customerId, periodMonth, cashFlow, List.of());
        }

        BigDecimal currentBalance = activeAccounts.stream()
                .map(Account::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Product> candidates = productRepository.findByProductStatus(ProductStatus.SELLING).stream()
                .filter(product -> isRecommendable(product, currentBalance, monthlySavings))
                .toList();

        if (candidates.isEmpty()) {
            return new ProductRecommendResponse(customerId, periodMonth, cashFlow, List.of());
        }

        List<Long> productIds = candidates.stream().map(Product::getProductId).toList();
        List<ProductInterestRate> activeRates = productInterestRateRepository
                .findByProductIdInAndIsActive(productIds, true)
                .stream()
                .toList();
        Map<Long, BigDecimal> bestRateMap = calculateBestRateMap(candidates, activeRates);

        List<ScoredProduct> scoredProducts = candidates.stream()
                .map(product -> scoreProduct(
                        product,
                        currentBalance,
                        monthlySavings,
                        transactions.size(),
                        periodMonth,
                        bestRateMap.getOrDefault(product.getProductId(), product.getBaseInterestRate())))
                .toList();

        BigDecimal maxExpectedReturn = scoredProducts.stream()
                .map(ScoredProduct::expectedReturn)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        List<RecommendedProduct> recommendations = scoredProducts.stream()
                .map(scored -> scored.withExpectedReturnScore(
                        normalizedExpectedReturnScore(scored.expectedReturn(), maxExpectedReturn)))
                .map(ScoredProduct::withTotalScore)
                .sorted(Comparator.comparing(ScoredProduct::totalScore).reversed())
                .limit(5)
                .map(this::toRecommended)
                .toList();

        return new ProductRecommendResponse(customerId, periodMonth, cashFlow, recommendations);
    }

    private BigDecimal sumByDirection(List<Transaction> transactions, DirectionType direction) {
        return transactions.stream()
                .filter(t -> t.getDirectionType() == direction)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isRecommendable(Product product, BigDecimal currentBalance, BigDecimal monthlySavings) {
        if (product.getProductType() == ProductType.SUBSCRIPTION) {
            return false;
        }
        if (isSpecialTargetProduct(product)) {
            return false;
        }
        if (isYouthProduct(product) && getCustomerAge() > YOUTH_MAX_AGE) {
            return false;
        }

        BigDecimal availableAmount = product.getProductType() == ProductType.DEPOSIT
                ? currentBalance
                : monthlySavings;
        return isJoinAmountAvailable(product, availableAmount);
    }

    private boolean isSpecialTargetProduct(Product product) {
        String text = searchableText(product);
        return text.contains("군인") || text.contains("장병") || text.contains("군무원");
    }

    private boolean isYouthProduct(Product product) {
        String text = searchableText(product);
        return text.contains("youth") || text.contains("young") || text.contains("청년");
    }

    private String searchableText(Product product) {
        return ((product.getProductName() == null ? "" : product.getProductName()) + " "
                + (product.getDescription() == null ? "" : product.getDescription()))
                .toLowerCase(Locale.ROOT);
    }

    private int getCustomerAge() {
        return OffsetDateTime.now(clock).getYear() - CUSTOMER_BIRTH_YEAR;
    }

    private boolean isJoinAmountAvailable(Product product, BigDecimal amount) {
        BigDecimal minAmount = product.getMinJoinAmount();
        BigDecimal maxAmount = product.getMaxJoinAmount();
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return false;
        }
        return maxAmount == null || amount.compareTo(maxAmount) <= 0;
    }

    private Map<Long, BigDecimal> calculateBestRateMap(List<Product> products, List<ProductInterestRate> activeRates) {
        Map<Long, BigDecimal> baseRateMap = activeRates.stream()
                .filter(rate -> rate.getRateType() == RateType.BASE || rate.getRateType() == RateType.PERIOD_BASE)
                .collect(Collectors.toMap(
                        ProductInterestRate::getProductId,
                        ProductInterestRate::getRate,
                        BigDecimal::max
                ));
        Map<Long, BigDecimal> preferentialRateMap = activeRates.stream()
                .filter(rate -> rate.getRateType() == RateType.PREFERENTIAL)
                .collect(Collectors.toMap(
                        ProductInterestRate::getProductId,
                        ProductInterestRate::getRate,
                        BigDecimal::add
                ));

        return products.stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        product -> baseRateMap
                                .getOrDefault(product.getProductId(), product.getBaseInterestRate())
                                .add(preferentialRateMap.getOrDefault(product.getProductId(), BigDecimal.ZERO))
                ));
    }

    private ScoredProduct scoreProduct(Product product,
                                       BigDecimal currentBalance,
                                       BigDecimal monthlySavings,
                                       int transactionCount,
                                       int periodMonth,
                                       BigDecimal bestRate) {
        BigDecimal financialFitScore = calculateFinancialFitScore(product, currentBalance, monthlySavings);
        BigDecimal expectedReturn = calculateExpectedReturn(product, currentBalance, monthlySavings, periodMonth, bestRate);
        BigDecimal liquidityScore = calculateLiquidityScore(product, transactionCount);
        BigDecimal benefitScore = calculateBenefitScore(product);

        return new ScoredProduct(
                product,
                bestRate,
                monthlySavings,
                expectedReturn,
                financialFitScore,
                BigDecimal.ZERO,
                liquidityScore,
                benefitScore,
                BigDecimal.ZERO
        );
    }

    private BigDecimal calculateFinancialFitScore(Product product, BigDecimal currentBalance, BigDecimal monthlySavings) {
        BigDecimal minJoinAmount = positiveOrOne(product.getMinJoinAmount());
        BigDecimal fitRatio;
        if (product.getProductType() == ProductType.DEPOSIT) {
            fitRatio = currentBalance.divide(minJoinAmount, 4, RoundingMode.HALF_UP);
        } else {
            fitRatio = monthlySavings.divide(minJoinAmount.multiply(BigDecimal.valueOf(2)), 4, RoundingMode.HALF_UP);
        }

        BigDecimal score = cap(fitRatio, BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                .multiply(FINANCIAL_FIT_MAX);
        if (isSavingsGrowthType(currentBalance, monthlySavings) && product.getProductType() == ProductType.SAVINGS) {
            score = score.multiply(SAVINGS_GROWTH_WEIGHT);
        }
        return cap(score, FINANCIAL_FIT_MAX);
    }

    private boolean isSavingsGrowthType(BigDecimal currentBalance, BigDecimal monthlySavings) {
        return monthlySavings.multiply(BigDecimal.valueOf(12)).compareTo(currentBalance) > 0;
    }

    private BigDecimal calculateExpectedReturn(Product product,
                                               BigDecimal currentBalance,
                                               BigDecimal monthlySavings,
                                               int requestedPeriodMonth,
                                               BigDecimal bestRate) {
        int period = resolvePeriodMonth(product, requestedPeriodMonth);
        BigDecimal yearlyRate = bestRate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        if (product.getProductType() == ProductType.DEPOSIT) {
            return currentBalance
                    .multiply(yearlyRate)
                    .multiply(BigDecimal.valueOf(period))
                    .divide(BigDecimal.valueOf(12), 0, RoundingMode.DOWN);
        }

        BigDecimal accumulatedWeightedMonths = BigDecimal.valueOf((long) period * (period + 1))
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        return monthlySavings
                .multiply(accumulatedWeightedMonths)
                .multiply(yearlyRate)
                .divide(BigDecimal.valueOf(12), 0, RoundingMode.DOWN);
    }

    private int resolvePeriodMonth(Product product, int requestedPeriodMonth) {
        int minPeriod = product.getMinPeriodMonth() == null ? 1 : product.getMinPeriodMonth();
        int maxPeriod = product.getMaxPeriodMonth() == null
                ? Math.max(requestedPeriodMonth, 12)
                : product.getMaxPeriodMonth();
        return Math.max(minPeriod, Math.min(maxPeriod, Math.max(requestedPeriodMonth, 12)));
    }

    private BigDecimal normalizedExpectedReturnScore(BigDecimal expectedReturn, BigDecimal maxExpectedReturn) {
        if (maxExpectedReturn.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return expectedReturn
                .divide(maxExpectedReturn, 4, RoundingMode.HALF_UP)
                .multiply(EXPECTED_RETURN_MAX);
    }

    private BigDecimal calculateLiquidityScore(Product product, int transactionCount) {
        int period = product.getMaxPeriodMonth() == null
                ? (product.getMinPeriodMonth() == null ? 12 : product.getMinPeriodMonth())
                : product.getMaxPeriodMonth();
        BigDecimal score;
        if (transactionCount <= LOW_TRANSACTION_COUNT) {
            score = period >= 24 ? LIQUIDITY_MAX : period >= 12 ? BigDecimal.valueOf(14) : BigDecimal.valueOf(8);
        } else if (transactionCount >= HIGH_TRANSACTION_COUNT) {
            score = period <= 12 ? LIQUIDITY_MAX : period <= 24 ? BigDecimal.valueOf(14) : BigDecimal.valueOf(8);
        } else {
            score = BigDecimal.valueOf(14);
        }
        if (Boolean.TRUE.equals(product.getIsEarlyTerminationAllowed())) {
            score = score.add(BigDecimal.valueOf(2));
        }
        return cap(score, LIQUIDITY_MAX);
    }

    private BigDecimal calculateBenefitScore(Product product) {
        BigDecimal score = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(product.getIsTaxBenefitAvailable())) {
            score = score.add(BigDecimal.valueOf(6));
        }
        if (Boolean.TRUE.equals(product.getIsEarlyTerminationAllowed())) {
            score = score.add(BigDecimal.valueOf(4));
        }
        return cap(score, BENEFIT_MAX);
    }

    private RecommendedProduct toRecommended(ScoredProduct scoredProduct) {
        Product product = scoredProduct.product();
        String monthlySavingsText = NumberFormat.getNumberInstance(Locale.KOREA)
                .format(scoredProduct.monthlySavings()) + "원";
        String reason = String.format(
                "저축 성장형 진단: 월 평균 저축 여력(%s)과 현재 잔액을 함께 반영했습니다. " +
                        "총점 %s점(재정 %s, 수익 %s, 유동성 %s, 혜택 %s). %s%% 금리 기준 추천.",
                monthlySavingsText,
                formatScore(scoredProduct.totalScore()),
                formatScore(scoredProduct.financialFitScore()),
                formatScore(scoredProduct.expectedReturnScore()),
                formatScore(scoredProduct.liquidityScore()),
                formatScore(scoredProduct.benefitScore()),
                scoredProduct.bestRate().toPlainString());

        return new RecommendedProduct(
                product.getProductId(),
                product.getProductName(),
                product.getProductType().name(),
                product.getBaseInterestRate(),
                scoredProduct.bestRate(),
                product.getMinJoinAmount(),
                product.getMaxJoinAmount(),
                product.getMinPeriodMonth(),
                product.getMaxPeriodMonth(),
                reason
        );
    }

    private BigDecimal positiveOrOne(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private BigDecimal cap(BigDecimal value, BigDecimal max) {
        return value.compareTo(max) > 0 ? max : value;
    }

    private String formatScore(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private ProductRecommendResponse emptyResult(String customerId, int periodMonth) {
        CashFlowSummary zero = new CashFlowSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return new ProductRecommendResponse(customerId, periodMonth, zero, List.of());
    }

    private record ScoredProduct(
            Product product,
            BigDecimal bestRate,
            BigDecimal monthlySavings,
            BigDecimal expectedReturn,
            BigDecimal financialFitScore,
            BigDecimal expectedReturnScore,
            BigDecimal liquidityScore,
            BigDecimal benefitScore,
            BigDecimal totalScore
    ) {
        private ScoredProduct withExpectedReturnScore(BigDecimal score) {
            return new ScoredProduct(
                    product,
                    bestRate,
                    monthlySavings,
                    expectedReturn,
                    financialFitScore,
                    score,
                    liquidityScore,
                    benefitScore,
                    totalScore
            );
        }

        private ScoredProduct withTotalScore() {
            return new ScoredProduct(
                    product,
                    bestRate,
                    monthlySavings,
                    expectedReturn,
                    financialFitScore,
                    expectedReturnScore,
                    liquidityScore,
                    benefitScore,
                    financialFitScore.add(expectedReturnScore).add(liquidityScore).add(benefitScore)
            );
        }
    }
}
