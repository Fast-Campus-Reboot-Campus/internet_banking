package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.entity.ProductInterestRate;
import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.AccountStatus;
import com.bank.deposit.domain.enums.DirectionType;
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
import java.util.stream.Collectors;

/**
 * 현금흐름 기반 수신 상품 추천 서비스.
 *
 * <p>고객의 실제 거래 내역(입출금 합계)을 분석해 월 평균 잉여자금을 계산하고,
 * 해당 금액에 적합한 수신 상품을 금리 내림차순으로 최대 5개 추천한다.
 *
 * <p>LLM 호출 없이 순수 규칙 기반으로 동작한다. 자연어 기반 추천이 필요하면
 * consultation-service 의 CASH_FLOW_RECOMMEND 피처를 이용할 것.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashflowBasedRecommendService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final ProductInterestRateRepository productInterestRateRepository;
    private final Clock clock;

    public ProductRecommendResponse recommend(String customerId, int periodMonth) {
        // 서비스 레벨 가드 — 다른 호출자(Kafka, Scheduler 등)에서도 안전하게
        Assert.isTrue(periodMonth >= 1, "periodMonth 는 1 이상이어야 합니다.");

        List<Long> accountIds = accountRepository.findByCustomerId(customerId).stream()
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
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

        BigDecimal totalInflow  = sumByDirection(transactions, DirectionType.IN);
        BigDecimal totalOutflow = sumByDirection(transactions, DirectionType.OUT);
        BigDecimal netCashFlow  = totalInflow.subtract(totalOutflow);
        BigDecimal estimatedSavingsAmount = netCashFlow.divide(
                BigDecimal.valueOf(periodMonth), 0, RoundingMode.DOWN);

        CashFlowSummary cashFlow = new CashFlowSummary(
                totalInflow, totalOutflow, netCashFlow, estimatedSavingsAmount);

        if (estimatedSavingsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ProductRecommendResponse(customerId, periodMonth, cashFlow, List.of());
        }

        // ── 상품 필터링 (SQL 조건으로 푸시다운) ─────────────────────────────────
        List<Product> candidates = productRepository
                .findSellingProductsByJoinAmount(estimatedSavingsAmount);

        if (candidates.isEmpty()) {
            return new ProductRecommendResponse(customerId, periodMonth, cashFlow, List.of());
        }

        // ── N+1 방지: 금리를 한 번의 IN 쿼리로 조회 ─────────────────────────────
        List<Long> productIds = candidates.stream().map(Product::getProductId).toList();
        Map<Long, BigDecimal> bestRateMap = productInterestRateRepository
                .findByProductIdInAndIsActive(productIds, true)
                .stream()
                .collect(Collectors.toMap(
                        ProductInterestRate::getProductId,
                        ProductInterestRate::getRate,
                        // 동일 상품에 여러 금리가 있으면 최고 금리 유지
                        BigDecimal::max
                ));

        List<RecommendedProduct> recommendations = candidates.stream()
                .map(p -> toRecommended(p, estimatedSavingsAmount, bestRateMap))
                .sorted(Comparator.comparing(RecommendedProduct::bestRate).reversed())
                .limit(5)
                .toList();

        return new ProductRecommendResponse(customerId, periodMonth, cashFlow, recommendations);
    }

    private BigDecimal sumByDirection(List<Transaction> transactions, DirectionType direction) {
        return transactions.stream()
                .filter(t -> t.getDirectionType() == direction)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RecommendedProduct toRecommended(Product product, BigDecimal estimatedSavingsAmount,
                                             Map<Long, BigDecimal> bestRateMap) {
        BigDecimal bestRate = bestRateMap.getOrDefault(
                product.getProductId(), product.getBaseInterestRate());

        String formattedAmount = NumberFormat.getNumberInstance(Locale.KOREA)
                .format(estimatedSavingsAmount) + "원";
        String reason = String.format("월 평균 저축 가능 금액(%s) 기반 추천. 연 %s%% 금리 적용.",
                formattedAmount, bestRate.toPlainString());

        return new RecommendedProduct(
                product.getProductId(),
                product.getProductName(),
                product.getProductType().name(),
                product.getBaseInterestRate(),
                bestRate,
                product.getMinJoinAmount(),
                product.getMaxJoinAmount(),
                product.getMinPeriodMonth(),
                product.getMaxPeriodMonth(),
                reason
        );
    }

    private ProductRecommendResponse emptyResult(String customerId, int periodMonth) {
        CashFlowSummary zero = new CashFlowSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return new ProductRecommendResponse(customerId, periodMonth, zero, List.of());
    }
}
