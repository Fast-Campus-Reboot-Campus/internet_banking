package com.bank.deposit.config;

import com.bank.deposit.domain.entity.*;
import com.bank.deposit.domain.enums.*;
import com.bank.deposit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final ContractAppliedRateRepository contractAppliedRateRepository;
    private final ContractRepository contractRepository;
    private final ContractSpecialTermAgreementRepository agreementRepository;
    private final DepartmentRepository departmentRepository;
    private final DepositProductRepository depositProductRepository;
    private final InterestHistoryRepository interestHistoryRepository;
    private final ProductInterestRateRepository interestRateRepository;
    private final ProductJoinChannelRepository joinChannelRepository;
    private final ProductRepository productRepository;
    private final ProductSpecialTermRepository productSpecialTermRepository;
    private final ProductTargetGroupRepository productTargetGroupRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final SpecialTermRepository specialTermRepository;
    private final SubscriptionProductRepository subscriptionProductRepository;
    private final TargetGroupRepository targetGroupRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        Department productDepartment = departmentRepository.save(Department.builder()
                .departmentCode("DEP-PRODUCT")
                .departmentName("수신상품부")
                .departmentType(DepartmentType.PRODUCT)
                .build());

        Product deposit = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("포스트맨 정기예금")
                .description("Postman 테스트용 정기예금 상품")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("3.20"))
                .minJoinAmount(bd("100000"))
                .maxJoinAmount(bd("100000000"))
                .minPeriodMonth(6)
                .maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true)
                .isTaxBenefitAvailable(true)
                .isAutoRenewalAvailable(true)
                .releasedAt("20260101")
                .build());

        Product savings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("포스트맨 자유적금")
                .description("Postman 테스트용 자유적금 상품")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("3.60"))
                .minJoinAmount(bd("10000"))
                .maxJoinAmount(bd("50000000"))
                .minPeriodMonth(6)
                .maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101")
                .build());

        Product subscription = productRepository.save(Product.builder()
                .productType(ProductType.SUBSCRIPTION)
                .productName("포스트맨 청약저축")
                .description("Postman 테스트용 청약 상품")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.80"))
                .minJoinAmount(bd("20000"))
                .maxJoinAmount(bd("10000000"))
                .minPeriodMonth(12)
                .maxPeriodMonth(120)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101")
                .build());

        depositProductRepository.save(DepositProduct.builder()
                .productId(deposit.getProductId())
                .depositType(DepositType.TERM)
                .isCompoundInterest(false)
                .build());
        savingsProductRepository.save(SavingsProduct.builder()
                .productId(savings.getProductId())
                .savingType(SavingType.FREE)
                .monthlyPaymentMinAmount(bd("10000"))
                .monthlyPaymentMaxAmount(bd("1000000"))
                .build());
        subscriptionProductRepository.save(SubscriptionProduct.builder()
                .productId(subscription.getProductId())
                .monthlyPaymentAmount(bd("100000"))
                .minMonthlyPayment(bd("20000"))
                .maxMonthlyPayment(bd("500000"))
                .maxRecognizedPaymentAmount(bd("10000000"))
                .build());

        TargetGroup personal = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("개인고객")
                .description("개인 인터넷뱅킹 고객")
                .build());
        TargetGroup worker = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("직장인")
                .description("급여소득이 있는 고객")
                .build());

        productTargetGroupRepository.save(ProductTargetGroup.builder()
                .id(new ProductTargetGroupId(deposit.getProductId(), personal.getTargetGroupId()))
                .build());
        productTargetGroupRepository.save(ProductTargetGroup.builder()
                .id(new ProductTargetGroupId(savings.getProductId(), personal.getTargetGroupId()))
                .build());
        productTargetGroupRepository.save(ProductTargetGroup.builder()
                .id(new ProductTargetGroupId(savings.getProductId(), worker.getTargetGroupId()))
                .build());

        joinChannelRepository.save(ProductJoinChannel.builder()
                .productId(deposit.getProductId())
                .joinChannelCode(JoinChannel.WEB)
                .build());
        joinChannelRepository.save(ProductJoinChannel.builder()
                .productId(deposit.getProductId())
                .joinChannelCode(JoinChannel.MOBILE)
                .build());
        joinChannelRepository.save(ProductJoinChannel.builder()
                .productId(savings.getProductId())
                .joinChannelCode(JoinChannel.WEB)
                .build());

        ProductInterestRate depositBaseRate = interestRateRepository.save(ProductInterestRate.builder()
                .productId(deposit.getProductId())
                .rateType(RateType.BASE)
                .minimumContractPeriod(6)
                .maximumContractPeriod(36)
                .minimumJoinAmount(bd("100000"))
                .maximumJoinAmount(bd("100000000"))
                .rate(bd("3.20"))
                .conditionDescription("정기예금 기본 금리")
                .effectiveStartDate("20260101")
                .build());
        ProductInterestRate depositPreferentialRate = interestRateRepository.save(ProductInterestRate.builder()
                .productId(deposit.getProductId())
                .rateType(RateType.PREFERENTIAL)
                .minimumContractPeriod(12)
                .minimumJoinAmount(bd("1000000"))
                .rate(bd("0.30"))
                .conditionDescription("비대면 가입 우대")
                .effectiveStartDate("20260101")
                .build());
        ProductInterestRate savingsBaseRate = interestRateRepository.save(ProductInterestRate.builder()
                .productId(savings.getProductId())
                .rateType(RateType.BASE)
                .minimumContractPeriod(6)
                .maximumContractPeriod(36)
                .minimumJoinAmount(bd("10000"))
                .maximumJoinAmount(bd("50000000"))
                .rate(bd("3.60"))
                .conditionDescription("자유적금 기본 금리")
                .effectiveStartDate("20260101")
                .build());

        SpecialTerm onlineTerm = specialTermRepository.save(SpecialTerm.builder()
                .specialTermName("비대면 가입 특약")
                .specialTermContent("비대면 채널로 가입하는 경우 적용되는 테스트 특약입니다.")
                .specialTermSummary("비대면 가입 우대 조건")
                .isRequired(true)
                .specialTermVersion("1.0")
                .startedAt("20260101")
                .build());
        SpecialTerm autoTransferTerm = specialTermRepository.save(SpecialTerm.builder()
                .specialTermName("자동이체 우대 특약")
                .specialTermContent("자동이체를 설정한 경우 적용되는 테스트 특약입니다.")
                .specialTermSummary("자동이체 우대 조건")
                .specialTermVersion("1.0")
                .startedAt("20260101")
                .build());

        productSpecialTermRepository.save(ProductSpecialTerm.builder()
                .productId(deposit.getProductId())
                .specialTermId(onlineTerm.getSpecialTermId())
                .isRequired(true)
                .build());
        productSpecialTermRepository.save(ProductSpecialTerm.builder()
                .productId(savings.getProductId())
                .specialTermId(autoTransferTerm.getSpecialTermId())
                .build());

        Contract depositContract = contractRepository.save(Contract.builder()
                .contractNumber("CTR-20260519-0001")
                .customerId("CUST001")
                .productId(deposit.getProductId())
                .joinAmount(bd("1000000"))
                .contractInterestRate(bd("3.20"))
                .totalPreferentialRate(bd("0.30"))
                .finalInterestRate(bd("3.50"))
                .taxBenefitType(TaxBenefitType.GENERAL)
                .contractPeriodMonth(12)
                .startedAt("20260519")
                .maturityAt("20270519")
                .isAutoRenewal(true)
                .joinChannel(JoinChannel.WEB)
                .build());
        Contract savingsContract = contractRepository.save(Contract.builder()
                .contractNumber("CTR-20260519-0002")
                .customerId("CUST001")
                .productId(savings.getProductId())
                .joinAmount(bd("50000"))
                .contractInterestRate(bd("3.60"))
                .finalInterestRate(bd("3.60"))
                .taxBenefitType(TaxBenefitType.GENERAL)
                .contractPeriodMonth(24)
                .startedAt("20260519")
                .maturityAt("20280519")
                .autoTransferEnabled(true)
                .autoTransferDay(25)
                .joinChannel(JoinChannel.MOBILE)
                .build());
        contractRepository.save(Contract.builder()
                .contractNumber("CTR-20260519-0003")
                .customerId("CUST_NO_ACCOUNT")
                .productId(deposit.getProductId())
                .joinAmount(bd("300000"))
                .contractInterestRate(bd("3.20"))
                .finalInterestRate(bd("3.20"))
                .taxBenefitType(TaxBenefitType.GENERAL)
                .contractPeriodMonth(12)
                .startedAt("20260519")
                .maturityAt("20270519")
                .joinChannel(JoinChannel.WEB)
                .build());

        Account depositAccount = accountRepository.save(Account.builder()
                .accountNumber("ACC-20260519-0001")
                .customerId("CUST001")
                .contractId(depositContract.getContractId())
                .accountType(ProductType.DEPOSIT)
                .accountAlias("포스트맨 예금계좌")
                .balance(bd("1000000"))
                .totalPaidAmount(bd("1000000"))
                .accountPassword("1234")
                .dailyWithdrawLimit(bd("10000000"))
                .dailyWithdrawCountLimit(10)
                .atmWithdrawLimit(bd("1000000"))
                .isOnlineBankingEnabled(true)
                .isMobileBankingEnabled(true)
                .openedAt("20260519")
                .maturityAt("20270519")
                .build());
        accountRepository.save(Account.builder()
                .accountNumber("ACC-20260519-0002")
                .customerId("CUST001")
                .contractId(savingsContract.getContractId())
                .accountType(ProductType.SAVINGS)
                .savingType(SavingType.FREE)
                .accountAlias("포스트맨 적금계좌")
                .balance(bd("100000"))
                .totalPaidAmount(bd("100000"))
                .accountPassword("1234")
                .dailyWithdrawLimit(bd("5000000"))
                .dailyWithdrawCountLimit(10)
                .atmWithdrawLimit(bd("500000"))
                .isOnlineBankingEnabled(true)
                .isMobileBankingEnabled(true)
                .openedAt("20260519")
                .maturityAt("20280519")
                .build());

        contractAppliedRateRepository.save(ContractAppliedRate.builder()
                .contractId(depositContract.getContractId())
                .rateId(depositBaseRate.getRateId())
                .appliedRate(bd("3.20"))
                .conditionVerifiedYn(true)
                .build());
        contractAppliedRateRepository.save(ContractAppliedRate.builder()
                .contractId(depositContract.getContractId())
                .rateId(depositPreferentialRate.getRateId())
                .appliedRate(bd("0.30"))
                .conditionVerifiedYn(true)
                .build());
        contractAppliedRateRepository.save(ContractAppliedRate.builder()
                .contractId(savingsContract.getContractId())
                .rateId(savingsBaseRate.getRateId())
                .appliedRate(bd("3.60"))
                .conditionVerifiedYn(true)
                .build());

        agreementRepository.save(ContractSpecialTermAgreement.builder()
                .contractId(depositContract.getContractId())
                .specialTermId(onlineTerm.getSpecialTermId())
                .isAgreed(true)
                .agreedAt("20260519")
                .agreementIpAddress("127.0.0.1")
                .agreementDeviceInfo("Postman")
                .isElectronicSigned(true)
                .build());

        OffsetDateTime now = OffsetDateTime.now();
        interestHistoryRepository.save(InterestHistory.builder()
                .contractId(depositContract.getContractId())
                .accountId(depositAccount.getAccountId())
                .appliedInterestRate(bd("3.50"))
                .interestCalculationStartDate("20260519")
                .interestCalculationEndDate("20260619")
                .interestOccurredAt(now)
                .interestAmount(bd("2468"))
                .taxBenefitType(TaxBenefitType.GENERAL)
                .appliedTaxRate(bd("15.4000"))
                .interestBeforeTax(bd("2917"))
                .interestTaxAmount(bd("404"))
                .localIncomeTaxAmount(bd("45"))
                .interestAfterTax(bd("2468"))
                .interestReason(InterestReason.REGULAR_INTEREST)
                .interestPaidAt(now)
                .build());
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
