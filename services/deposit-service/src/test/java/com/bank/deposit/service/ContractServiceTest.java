package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Contract;
import com.bank.deposit.domain.entity.ContractAppliedRate;
import com.bank.deposit.domain.entity.ContractSpecialTermAgreement;
import com.bank.deposit.domain.entity.Product;
import com.bank.deposit.domain.enums.*;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService")
class ContractServiceTest {

    private ContractService contractService;

    @Mock private ContractRepository contractRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ContractAppliedRateRepository appliedRateRepository;
    @Mock private ContractSpecialTermAgreementRepository agreementRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        contractService = new ContractService(contractRepository, accountRepository, productRepository,
                appliedRateRepository, agreementRepository, passwordEncoder, clock);
        org.mockito.Mockito.lenient()
                .when(passwordEncoder.encode(org.mockito.ArgumentMatchers.any()))
                .thenReturn("$2a$10$abcdefghijklmnopqrstuu9QwmFAnNd0x5QyZ9LhQOW7bpcE6Pj2a");
        org.mockito.Mockito.lenient()
                .when(accountRepository.nextAccountNumberSequenceValue())
                .thenReturn(100000000001L);
    }

    @Nested
    @DisplayName("계약 목록 조회")
    class FindAll {

        @Test
        @DisplayName("필터 없이 전체 계약을 조회한다")
        void findAllNoFilter() {
            given(contractRepository.findAll()).willReturn(List.of(contract("CUST-001")));

            List<Contract> result = contractService.findAll(null, null);

            assertThat(result).hasSize(1);
            then(contractRepository).should().findAll();
        }

        @Test
        @DisplayName("고객 ID와 상태로 계약을 필터링한다")
        void findAllByCustomerAndStatus() {
            given(contractRepository.findByCustomerIdAndContractStatus("CUST-001", ContractStatus.ACTIVE))
                    .willReturn(List.of(contract("CUST-001")));

            List<Contract> result = contractService.findAll("CUST-001", ContractStatus.ACTIVE);

            assertThat(result).hasSize(1);
            then(contractRepository).should()
                    .findByCustomerIdAndContractStatus("CUST-001", ContractStatus.ACTIVE);
        }

        @Test
        @DisplayName("고객 ID만으로 계약을 조회한다")
        void findAllByCustomer() {
            given(contractRepository.findByCustomerId("CUST-001"))
                    .willReturn(List.of(contract("CUST-001")));

            List<Contract> result = contractService.findAll("CUST-001", null);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("계약 단건 조회")
    class FindById {

        @Test
        @DisplayName("존재하는 계약을 조회한다")
        void findById() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));

            Contract result = contractService.findById(1L);

            assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        }

        @Test
        @DisplayName("존재하지 않으면 예외가 발생한다")
        void findByIdNotFound() {
            given(contractRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.findById(1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("계약 생성")
    class CreateContract {

        @Test
        @DisplayName("판매 중인 상품으로 계약을 생성한다")
        void createContract() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sellingProduct()));
            given(contractRepository.save(any(Contract.class))).willAnswer(inv -> inv.getArgument(0));
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Contract result = contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(1_000_000), 12,
                    JoinChannel.WEB, null, null, null, false, false, null,
                    null, null, null, null, "1234");

            assertThat(result.getCustomerId()).isEqualTo("CUST-001");
            assertThat(result.getContractNumber()).startsWith("CTR-");
            assertThat(result.getContractStatus()).isEqualTo(ContractStatus.ACTIVE);
        }

        @Test
        @DisplayName("판매 종료된 상품으로 계약 생성 시 예외가 발생한다")
        void createWithSuspendedProduct() {
            Product suspended = Product.builder()
                    .productType(ProductType.DEPOSIT)
                    .productName("판매종료 상품")
                    .baseInterestRate(BigDecimal.valueOf(2.0))
                    .productStatus(ProductStatus.SUSPENDED)
                    .build();
            given(productRepository.findById(1L)).willReturn(Optional.of(suspended));

            assertThatThrownBy(() -> contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(1_000_000), 12,
                    null, null, null, null, false, false, null,
                    null, null, null, null, "1234"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("계좌 비밀번호가 없으면 계약 생성 시 예외가 발생한다")
        void createWithoutPassword() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sellingProduct()));

            assertThatThrownBy(() -> contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(1_000_000), 12,
                    null, null, null, null, false, false, null,
                    null, null, null, null, null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("joinAmount가 minJoinAmount보다 작으면 예외가 발생한다")
        void createWithJoinAmountBelowMin() {
            Product product = Product.builder()
                    .productType(ProductType.DEPOSIT)
                    .productName("정기예금")
                    .baseInterestRate(BigDecimal.valueOf(3.0))
                    .productStatus(ProductStatus.SELLING)
                    .minJoinAmount(BigDecimal.valueOf(100_000))
                    .maxJoinAmount(BigDecimal.valueOf(100_000_000))
                    .build();
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            assertThatThrownBy(() -> contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(100), 12,
                    JoinChannel.WEB, null, null, null, false, false, null,
                    null, null, null, null, "1234"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최소 가입금액");
        }

        @Test
        @DisplayName("joinAmount가 maxJoinAmount보다 크면 예외가 발생한다")
        void createWithJoinAmountAboveMax() {
            Product product = Product.builder()
                    .productType(ProductType.DEPOSIT)
                    .productName("정기예금")
                    .baseInterestRate(BigDecimal.valueOf(3.0))
                    .productStatus(ProductStatus.SELLING)
                    .minJoinAmount(BigDecimal.valueOf(100_000))
                    .maxJoinAmount(BigDecimal.valueOf(100_000_000))
                    .build();
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            assertThatThrownBy(() -> contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(999_999_999), 12,
                    JoinChannel.WEB, null, null, null, false, false, null,
                    null, null, null, null, "1234"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대 가입금액");
        }

        @Test
        @DisplayName("joinAmount가 minJoinAmount와 같으면 계약이 정상 생성된다")
        void createWithJoinAmountEqualToMin() {
            Product product = Product.builder()
                    .productType(ProductType.DEPOSIT)
                    .productName("정기예금")
                    .baseInterestRate(BigDecimal.valueOf(3.0))
                    .productStatus(ProductStatus.SELLING)
                    .minJoinAmount(BigDecimal.valueOf(100_000))
                    .maxJoinAmount(BigDecimal.valueOf(100_000_000))
                    .build();
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(contractRepository.save(any(Contract.class))).willAnswer(inv -> inv.getArgument(0));
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Contract result = contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(100_000), 12,
                    JoinChannel.WEB, null, null, null, false, false, null,
                    null, null, null, null, "1234");

            assertThat(result.getJoinAmount()).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("우대금리가 기본금리에 합산되어 최종금리가 계산된다")
        void finalRateCalculation() {
            given(productRepository.findById(1L)).willReturn(Optional.of(sellingProduct()));
            given(contractRepository.save(any(Contract.class))).willAnswer(inv -> inv.getArgument(0));
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Contract result = contractService.createContract(
                    "CUST-001", 1L, BigDecimal.valueOf(1_000_000), 12,
                    JoinChannel.WEB, BigDecimal.valueOf(3.0), BigDecimal.valueOf(0.5),
                    null, false, false, null, null, null, null, null, "1234");

            assertThat(result.getFinalInterestRate()).isEqualByComparingTo("3.5");
        }
    }

    @Nested
    @DisplayName("계약 해지")
    class Terminate {

        @Test
        @DisplayName("활성 계약을 해지한다")
        void terminate() {
            given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));

            Contract result = contractService.terminate(1L, "고객 요청 해지");

            assertThat(result.getContractStatus()).isEqualTo(ContractStatus.TERMINATED);
            assertThat(result.getTerminationReason()).isEqualTo("고객 요청 해지");
        }

        @Test
        @DisplayName("이미 해지된 계약은 재해지 시 예외가 발생한다")
        void terminateAlreadyTerminated() {
            Contract terminated = Contract.builder()
                    .contractNumber("CTR-001")
                    .customerId("CUST-001")
                    .productId(1L)
                    .joinAmount(BigDecimal.valueOf(1_000_000))
                    .contractInterestRate(BigDecimal.valueOf(3.0))
                    .totalPreferentialRate(BigDecimal.ZERO)
                    .finalInterestRate(BigDecimal.valueOf(3.0))
                    .contractPeriodMonth(12)
                    .startedAt(java.time.LocalDate.of(2026, 1, 1))
                    .joinChannel(JoinChannel.WEB)
                    .contractStatus(ContractStatus.TERMINATED)
                    .build();
            given(contractRepository.findById(1L)).willReturn(Optional.of(terminated));

            assertThatThrownBy(() -> contractService.terminate(1L, "재해지"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("계약 상태를 MATURED로 변경한다")
    void mature() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));

        Contract result = contractService.mature(1L);

        assertThat(result.getContractStatus()).isEqualTo(ContractStatus.MATURED);
    }

    @Test
    @DisplayName("계약 상태를 변경한다")
    void changeStatus() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));

        Contract result = contractService.changeStatus(1L, ContractStatus.SUSPENDED);

        assertThat(result.getContractStatus()).isEqualTo(ContractStatus.SUSPENDED);
    }

    @Test
    @DisplayName("자동이체일을 변경한다")
    void updateAutoTransferDay() {
        Contract contract = contract("CUST-001");
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract));

        contractService.updateAutoTransferDay(1L, 25);

        assertThat(contract.getAutoTransferDay()).isEqualTo(25);
    }

    @Test
    @DisplayName("수신 상품 계약이면 수신 계약으로 조회한다")
    void findDepositContract() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(productRepository.findById(1L)).willReturn(Optional.of(sellingProduct()));

        Contract result = contractService.findDepositContract(1L);

        assertThat(result.getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("수신 상품이 아니면 수신 계약 조회 시 예외가 발생한다")
    void findDepositContractNotDepositProduct() {
        Product savingsProduct = Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("적금")
                .baseInterestRate(BigDecimal.valueOf(3.0))
                .productStatus(ProductStatus.SELLING)
                .build();
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(productRepository.findById(1L)).willReturn(Optional.of(savingsProduct));

        assertThatThrownBy(() -> contractService.findDepositContract(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("수신 계약 자동이체 설정을 수정한다")
    void updateDepositSettings() {
        Contract contract = contract("CUST-001");
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract));

        Contract result = contractService.updateDepositSettings(1L, true, 15);

        assertThat(result.getAutoTransferEnabled()).isTrue();
        assertThat(result.getAutoTransferDay()).isEqualTo(15);
    }

    @Test
    @DisplayName("계약 적용 금리 목록을 조회한다")
    void findAppliedRates() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(appliedRateRepository.findByContractId(1L))
                .willReturn(List.of(appliedRate()));

        List<ContractAppliedRate> result = contractService.findAppliedRates(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("계약 적용 금리를 저장한다")
    void saveAppliedRate() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(appliedRateRepository.save(any(ContractAppliedRate.class))).willAnswer(inv -> inv.getArgument(0));

        ContractAppliedRate result = contractService.saveAppliedRate(1L, 2L, BigDecimal.valueOf(0.3), true);

        assertThat(result.getContractId()).isEqualTo(1L);
        assertThat(result.getRateId()).isEqualTo(2L);
        assertThat(result.getConditionVerifiedYn()).isTrue();
    }

    @Test
    @DisplayName("우대 금리를 저장한다")
    void savePreferentialRate() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(appliedRateRepository.save(any(ContractAppliedRate.class))).willAnswer(inv -> inv.getArgument(0));

        ContractAppliedRate result = contractService.savePreferentialRate(
                1L, "급여이체", BigDecimal.valueOf(0.2), true);

        assertThat(result.getAppliedRate()).isEqualByComparingTo("0.2");
        assertThat(result.getConditionVerifiedYn()).isTrue();
    }

    @Test
    @DisplayName("적용 금리를 삭제한다")
    void deleteAppliedRate() {
        ContractAppliedRate rate = appliedRate();
        given(appliedRateRepository.findById(1L)).willReturn(Optional.of(rate));

        contractService.deleteAppliedRate(1L);

        then(appliedRateRepository).should().delete(rate);
    }

    @Test
    @DisplayName("적용 금리가 없으면 삭제 시 예외가 발생한다")
    void deleteAppliedRateNotFound() {
        given(appliedRateRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.deleteAppliedRate(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("계약 특약 동의 목록을 조회한다")
    void findAgreements() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(agreementRepository.findByContractId(1L))
                .willReturn(List.of(agreement()));

        List<ContractSpecialTermAgreement> result = contractService.findAgreements(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("계약 특약 동의를 저장한다")
    void agree() {
        given(contractRepository.findById(1L)).willReturn(Optional.of(contract("CUST-001")));
        given(agreementRepository.save(any(ContractSpecialTermAgreement.class))).willAnswer(inv -> inv.getArgument(0));

        ContractSpecialTermAgreement result = contractService.agree(
                1L, 7L, true, "20260521", "127.0.0.1", "WEB", true);

        assertThat(result.getContractId()).isEqualTo(1L);
        assertThat(result.getSpecialTermId()).isEqualTo(7L);
        assertThat(result.getIsAgreed()).isTrue();
        assertThat(result.getIsElectronicSigned()).isTrue();
    }

    @Test
    @DisplayName("계약 특약 동의를 철회한다")
    void withdrawAgreement() {
        ContractSpecialTermAgreement agreement = agreement();
        given(agreementRepository.findById(1L)).willReturn(Optional.of(agreement));

        ContractSpecialTermAgreement result = contractService.withdraw(1L, 1L);

        assertThat(result.getIsAgreementWithdrawn()).isTrue();
        assertThat(result.getAgreementWithdrawnAt()).isNotBlank();
    }

    // ── 픽스처 ──────────────────────────────────────────────────────────────

    private Contract contract(String customerId) {
        return Contract.builder()
                .contractNumber("CTR-001")
                .customerId(customerId)
                .productId(1L)
                .joinAmount(BigDecimal.valueOf(1_000_000))
                .contractInterestRate(BigDecimal.valueOf(3.0))
                .totalPreferentialRate(BigDecimal.ZERO)
                .finalInterestRate(BigDecimal.valueOf(3.0))
                .contractPeriodMonth(12)
                .startedAt(java.time.LocalDate.of(2026, 1, 1))
                .joinChannel(JoinChannel.WEB)
                .build();
    }

    private Product sellingProduct() {
        return Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("정기예금")
                .baseInterestRate(BigDecimal.valueOf(3.0))
                .productStatus(ProductStatus.SELLING)
                .build();
    }

    private ContractAppliedRate appliedRate() {
        return ContractAppliedRate.builder()
                .contractId(1L)
                .rateId(2L)
                .appliedRate(BigDecimal.valueOf(0.3))
                .conditionVerifiedYn(true)
                .build();
    }

    private ContractSpecialTermAgreement agreement() {
        return ContractSpecialTermAgreement.builder()
                .contractId(1L)
                .specialTermId(7L)
                .isAgreed(true)
                .agreedAt("20260521")
                .isElectronicSigned(true)
                .build();
    }
}
