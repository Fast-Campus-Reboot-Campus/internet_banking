package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.entity.ProductInterestRate;
import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.*;
import com.bank.deposit.dto.response.ProductRecommendResponse;
import com.bank.deposit.dto.response.RecommendedProduct;
import com.bank.deposit.repository.AccountRepository;
import com.bank.deposit.repository.ProductInterestRateRepository;
import com.bank.deposit.repository.ProductRepository;
import com.bank.deposit.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendAgentService")
class RecommendAgentServiceTest {

    @InjectMocks
    private RecommendAgentService recommendAgentService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductInterestRateRepository productInterestRateRepository;

    @Nested
    @DisplayName("recommend()")
    class Recommend {

        @Test
        @DisplayName("입금이 출금보다 많으면 추천 상품 목록을 반환한다")
        void normalRecommendation() {
            // given
            Account account = account("CUST001", 1L);
            given(accountRepository.findByCustomerId("CUST001"))
                    .willReturn(List.of(account));

            List<Transaction> transactions = List.of(
                    transaction(1L, DirectionType.IN,  new BigDecimal("1500000")),
                    transaction(1L, DirectionType.IN,  new BigDecimal("1500000")),
                    transaction(1L, DirectionType.IN,  new BigDecimal("1500000")),
                    transaction(1L, DirectionType.OUT, new BigDecimal("1000000")),
                    transaction(1L, DirectionType.OUT, new BigDecimal("1000000")),
                    transaction(1L, DirectionType.OUT, new BigDecimal("1000000"))
            );
            // totalInflow=4500000, totalOutflow=3000000, net=1500000, estimated=500000/월
            given(transactionRepository.findByAccountIdInAndTransactionAtBetweenAndStatus(
                    eq(List.of(1L)), any(OffsetDateTime.class), any(OffsetDateTime.class),
                    eq(TransactionStatus.SUCCESS)))
                    .willReturn(transactions);

            Product savings = product(1L, "자유적금", ProductType.SAVINGS,
                    new BigDecimal("3.20"), new BigDecimal("10000"), new BigDecimal("1000000"));
            Product deposit = product(2L, "정기예금", ProductType.DEPOSIT,
                    new BigDecimal("2.80"), new BigDecimal("100000"), new BigDecimal("5000000"));
            given(productRepository.findByProductStatus(ProductStatus.SELLING))
                    .willReturn(List.of(savings, deposit));

            given(productInterestRateRepository.findByProductIdAndIsActive(1L, true))
                    .willReturn(List.of(interestRate(1L, new BigDecimal("3.50"))));
            given(productInterestRateRepository.findByProductIdAndIsActive(2L, true))
                    .willReturn(List.of(interestRate(2L, new BigDecimal("3.00"))));

            // when
            ProductRecommendResponse result = recommendAgentService.recommend("CUST001", 3);

            // then
            assertThat(result.customerId()).isEqualTo("CUST001");
            assertThat(result.analysisPeriodMonth()).isEqualTo(3);
            assertThat(result.cashFlow().totalInflow()).isEqualByComparingTo("4500000");
            assertThat(result.cashFlow().totalOutflow()).isEqualByComparingTo("3000000");
            assertThat(result.cashFlow().netCashFlow()).isEqualByComparingTo("1500000");
            assertThat(result.cashFlow().estimatedSavingsAmount()).isEqualByComparingTo("500000");
            assertThat(result.cashFlow().estimatedSavingsAmount()).isPositive();
            assertThat(result.recommendations()).isNotEmpty().hasSize(2);
            RecommendedProduct first = result.recommendations().get(0);
            assertThat(first.productName()).isEqualTo(savings.getProductName());
            assertThat(first.reason()).isNotBlank()
                    .contains("500,000")
                    .contains("3.50");
            // bestRate 내림차순 정렬 확인
            assertThat(result.recommendations().get(0).bestRate()).isEqualByComparingTo("3.50");
            assertThat(result.recommendations().get(1).bestRate()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("고객 계좌가 없으면 cashFlow 전부 0, 빈 recommendations를 반환한다")
        void noAccounts() {
            given(accountRepository.findByCustomerId("CUST999"))
                    .willReturn(List.of());

            ProductRecommendResponse result = recommendAgentService.recommend("CUST999", 3);

            assertThat(result.customerId()).isEqualTo("CUST999");
            assertThat(result.cashFlow().totalInflow()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.cashFlow().estimatedSavingsAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.recommendations()).isEmpty();
        }

        @Test
        @DisplayName("거래내역이 없으면 cashFlow 전부 0, 빈 recommendations를 반환한다")
        void noTransactions() {
            given(accountRepository.findByCustomerId("CUST001"))
                    .willReturn(List.of(account("CUST001", 1L)));
            given(transactionRepository.findByAccountIdInAndTransactionAtBetweenAndStatus(
                    any(), any(), any(), any()))
                    .willReturn(List.of());

            ProductRecommendResponse result = recommendAgentService.recommend("CUST001", 3);

            assertThat(result.cashFlow().totalInflow()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.cashFlow().estimatedSavingsAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.recommendations()).isEmpty();
        }

        @Test
        @DisplayName("estimatedSavingsAmount가 minJoinAmount 미만이면 해당 상품은 추천하지 않는다")
        void amountConditionNotMet() {
            // estimatedSavingsAmount = 50,000원
            given(accountRepository.findByCustomerId("CUST001"))
                    .willReturn(List.of(account("CUST001", 1L)));
            given(transactionRepository.findByAccountIdInAndTransactionAtBetweenAndStatus(
                    any(), any(), any(), any()))
                    .willReturn(List.of(
                            transaction(1L, DirectionType.IN,  new BigDecimal("200000")),
                            transaction(1L, DirectionType.OUT, new BigDecimal("50000"))
                    ));
            // net=150000, 3개월 → estimated=50000
            // minJoinAmount=100000 상품은 필터에서 제외
            Product highMin = product(1L, "고액예금", ProductType.DEPOSIT,
                    new BigDecimal("3.50"), new BigDecimal("100000"), null);
            given(productRepository.findByProductStatus(ProductStatus.SELLING))
                    .willReturn(List.of(highMin));

            ProductRecommendResponse result = recommendAgentService.recommend("CUST001", 3);

            assertThat(result.recommendations()).isEmpty();
        }

        @Test
        @DisplayName("판매 중인 상품이 없으면 빈 recommendations를 반환한다")
        void noSellingProducts() {
            given(accountRepository.findByCustomerId("CUST001"))
                    .willReturn(List.of(account("CUST001", 1L)));
            given(transactionRepository.findByAccountIdInAndTransactionAtBetweenAndStatus(
                    any(), any(), any(), any()))
                    .willReturn(List.of(
                            transaction(1L, DirectionType.IN,  new BigDecimal("1000000")),
                            transaction(1L, DirectionType.OUT, new BigDecimal("400000"))
                    ));
            given(productRepository.findByProductStatus(ProductStatus.SELLING))
                    .willReturn(List.of());

            ProductRecommendResponse result = recommendAgentService.recommend("CUST001", 3);

            assertThat(result.recommendations()).isEmpty();
        }
    }

    // ── 픽스처 ──────────────────────────────────────────────────────────────

    private Account account(String customerId, Long accountId) {
        return Account.builder()
                .accountId(accountId)
                .accountNumber("ACC-" + accountId)
                .customerId(customerId)
                .contractId(accountId)
                .accountType(ProductType.DEPOSIT)
                .accountPassword("1234")
                .openedAt("20260101")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private Transaction transaction(Long accountId, DirectionType direction, BigDecimal amount) {
        return Transaction.builder()
                .transactionNumber("TX-" + System.nanoTime())
                .accountId(accountId)
                .transactionType(TransactionType.DEPOSIT)
                .directionType(direction)
                .amount(amount)
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(amount)
                .channelType(TransactionChannel.INTERNET)
                .status(TransactionStatus.SUCCESS)
                .transactionAt(OffsetDateTime.now())
                .build();
    }

    private Product product(Long productId, String name, ProductType type,
                            BigDecimal baseRate, BigDecimal minJoin, BigDecimal maxJoin) {
        return Product.builder()
                .productId(productId)
                .productName(name)
                .productType(type)
                .baseInterestRate(baseRate)
                .minJoinAmount(minJoin)
                .maxJoinAmount(maxJoin)
                .minPeriodMonth(6)
                .maxPeriodMonth(36)
                .productStatus(ProductStatus.SELLING)
                .build();
    }

    private ProductInterestRate interestRate(Long productId, BigDecimal rate) {
        return ProductInterestRate.builder()
                .productId(productId)
                .rateType(RateType.BASE)
                .rate(rate)
                .effectiveStartDate("20260101")
                .isActive(true)
                .build();
    }
}
