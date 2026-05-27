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

        // ── 대상 그룹 ──────────────────────────────────────────────────────────
        TargetGroup personal = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("개인고객")
                .description("개인 인터넷뱅킹 고객")
                .build());
        TargetGroup youth = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("청년고객")
                .description("만 19~34세 청년 고객")
                .build());
        TargetGroup military = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("국군장병")
                .description("현역 군인")
                .build());
        TargetGroup worker = targetGroupRepository.save(TargetGroup.builder()
                .targetGroupName("직장인")
                .description("급여소득이 있는 고객")
                .build());

        // ══════════════════════════════════════════════════════════════════════
        // TERM 정기예금 4개 (main 브랜치)
        // ══════════════════════════════════════════════════════════════════════
        Product axfulRegular = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("AXful 정기예금")
                .description("Digital AXful의 대표 정기예금")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.15"))
                .minJoinAmount(bd("1000000"))
                .minPeriodMonth(1).maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true).isTaxBenefitAvailable(true)
                .isAutoRenewalAvailable(true)
                .releasedAt("20260101").build());

        Product axfulSuper = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("AXful 수퍼정기예금(개인)")
                .description("가입 조건을 직접 설계하는")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.10"))
                .minJoinAmount(bd("1000000"))
                .minPeriodMonth(1).maxPeriodMonth(36)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        Product generalDeposit = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("일반정기예금")
                .description("목돈 모아 안정수익 마음든든")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.10"))
                .minJoinAmount(bd("1000000"))
                .minPeriodMonth(1).maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true).isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        Product youthLeap = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT)
                .productName("AXful 청년도약계좌")
                .description("청년의 자산형성을 응원합니다")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("3.50"))
                .minJoinAmount(bd("1000"))
                .minPeriodMonth(60).maxPeriodMonth(60)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        depositProductRepository.save(DepositProduct.builder()
                .productId(axfulRegular.getProductId()).depositType(DepositType.TERM).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder()
                .productId(axfulSuper.getProductId()).depositType(DepositType.TERM).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder()
                .productId(generalDeposit.getProductId()).depositType(DepositType.TERM).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder()
                .productId(youthLeap.getProductId()).depositType(DepositType.TERM).isCompoundInterest(false).build());

        // AXful 정기예금 기간별 BASE 7구간 + 우대 1개
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(2)