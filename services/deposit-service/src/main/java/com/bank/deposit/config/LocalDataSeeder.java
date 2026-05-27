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

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final DepositProductRepository depositProductRepository;
    private final ProductInterestRateRepository interestRateRepository;
    private final ProductJoinChannelRepository joinChannelRepository;
    private final ProductRepository productRepository;
    private final ProductTargetGroupRepository productTargetGroupRepository;
    private final SavingsProductRepository savingsProductRepository;
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

        // ══════════════════════════════════════════════════════════════════════
        // TERM 정기예금 4개
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
                .minimumJoinAmount(bd("1000000")).rate(bd("1.80"))
                .conditionDescription("1개월 이상~3개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(3).maximumContractPeriod(5)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.00"))
                .conditionDescription("3개월 이상~6개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(6).maximumContractPeriod(8)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.10"))
                .conditionDescription("6개월 이상~9개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(9).maximumContractPeriod(11)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.10"))
                .conditionDescription("9개월 이상~12개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(12).maximumContractPeriod(23)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.15"))
                .conditionDescription("12개월 이상~24개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(24).maximumContractPeriod(35)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.20"))
                .conditionDescription("24개월 이상~36개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(36).maximumContractPeriod(36)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.20"))
                .conditionDescription("36개월 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulRegular.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(3).maximumContractPeriod(23)
                .minimumJoinAmount(bd("1000000")).rate(bd("0.75"))
                .conditionDescription("비대면(인터넷·스타뱅킹) 가입 우대금리").effectiveStartDate("20260101").build());

        // AXful 수퍼정기예금(개인) BASE 4구간 + 우대 1개
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulSuper.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(5)
                .minimumJoinAmount(bd("1000000")).rate(bd("1.90"))
                .conditionDescription("1개월 이상~6개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulSuper.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(6).maximumContractPeriod(11)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.00"))
                .conditionDescription("6개월 이상~12개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulSuper.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(12).maximumContractPeriod(23)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.10"))
                .conditionDescription("12개월 이상~24개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulSuper.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(24).maximumContractPeriod(35)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.15"))
                .conditionDescription("24개월 이상~36개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(axfulSuper.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(12).maximumContractPeriod(35)
                .minimumJoinAmount(bd("1000000")).rate(bd("0.20"))
                .conditionDescription("비대면 가입 우대금리").effectiveStartDate("20260101").build());

        // 일반정기예금 BASE 3구간
        interestRateRepository.save(ProductInterestRate.builder().productId(generalDeposit.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(5)
                .minimumJoinAmount(bd("1000000")).rate(bd("1.85"))
                .conditionDescription("1개월 이상~6개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(generalDeposit.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(6).maximumContractPeriod(11)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.00"))
                .conditionDescription("6개월 이상~12개월 미만 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(generalDeposit.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(12).maximumContractPeriod(36)
                .minimumJoinAmount(bd("1000000")).rate(bd("2.10"))
                .conditionDescription("12개월 이상~36개월 기본금리").effectiveStartDate("20260101").build());

        // AXful 청년도약계좌 BASE + 우대 2개
        interestRateRepository.save(ProductInterestRate.builder().productId(youthLeap.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(60).maximumContractPeriod(60)
                .minimumJoinAmount(bd("1000")).rate(bd("3.50"))
                .conditionDescription("60개월 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthLeap.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(60).maximumContractPeriod(60)
                .minimumJoinAmount(bd("1000")).rate(bd("1.00"))
                .conditionDescription("기본 소득요건 충족 우대 (+1.0%)").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthLeap.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(60).maximumContractPeriod(60)
                .minimumJoinAmount(bd("1000")).rate(bd("1.50"))
                .conditionDescription("고소득 요건 추가 충족 우대 (+1.5%)").effectiveStartDate("20260101").build());

        // TERM 채널
        joinChannelRepository.save(ProductJoinChannel.builder().productId(axfulRegular.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(axfulRegular.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(axfulSuper.getProductId()).joinChannelCode(JoinChannel.BRANCH).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(generalDeposit.getProductId()).joinChannelCode(JoinChannel.BRANCH).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(youthLeap.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(youthLeap.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());

        // TERM 대상 그룹
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(axfulRegular.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(axfulSuper.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(generalDeposit.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthLeap.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthLeap.getProductId(), youth.getTargetGroupId())).build());

        // ══════════════════════════════════════════════════════════════════════
        // SAVINGS 적금 5개
        // ══════════════════════════════════════════════════════════════════════
        Product diySavings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("AXful 내맘대로적금")
                .description("누구나 쉽게 자유롭게 DIY")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.95"))
                .minJoinAmount(bd("10000")).maxJoinAmount(bd("50000000"))
                .minPeriodMonth(1).maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true).isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        Product dollarSavings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("AXful 달러자적금")
                .description("달러 가치상승 응원하는 두배이율")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("1.00"))
                .minJoinAmount(bd("10000")).maxJoinAmount(bd("10000000"))
                .minPeriodMonth(1).maxPeriodMonth(6)
                .isEarlyTerminationAllowed(true)
                .releasedAt("20260101").build());

        Product clearSkySavings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("AXful 맑은하늘적금")
                .description("맑은하늘 인증코드 금리도 Up")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.85"))
                .minJoinAmount(bd("10000")).maxJoinAmount(bd("50000000"))
                .minPeriodMonth(1).maxPeriodMonth(36)
                .isEarlyTerminationAllowed(true).isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        Product militarySavings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("AXful 장병내일준비적금")
                .description("국군장병 미래대비 앞날준비")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("5.00"))
                .minJoinAmount(bd("1000")).maxJoinAmount(bd("1000000"))
                .minPeriodMonth(24).maxPeriodMonth(24)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        Product specialSavings = productRepository.save(Product.builder()
                .productType(ProductType.SAVINGS)
                .productName("AXful 특★한 적금")
                .description("고객 모두의 높은 수익을 위한 특별한 준비")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.00"))
                .minJoinAmount(bd("10000")).maxJoinAmount(bd("30000000"))
                .minPeriodMonth(1).maxPeriodMonth(1)
                .releasedAt("20260101").build());

        savingsProductRepository.save(SavingsProduct.builder()
                .productId(diySavings.getProductId()).savingType(SavingType.FREE)
                .monthlyPaymentMinAmount(bd("10000")).monthlyPaymentMaxAmount(bd("1000000")).build());
        savingsProductRepository.save(SavingsProduct.builder()
                .productId(dollarSavings.getProductId()).savingType(SavingType.FREE)
                .monthlyPaymentMinAmount(bd("10000")).monthlyPaymentMaxAmount(bd("500000")).build());
        savingsProductRepository.save(SavingsProduct.builder()
                .productId(clearSkySavings.getProductId()).savingType(SavingType.FREE)
                .monthlyPaymentMinAmount(bd("10000")).monthlyPaymentMaxAmount(bd("1000000")).build());
        savingsProductRepository.save(SavingsProduct.builder()
                .productId(militarySavings.getProductId()).savingType(SavingType.REGULAR)
                .monthlyPaymentMinAmount(bd("1000")).monthlyPaymentMaxAmount(bd("100000")).build());
        savingsProductRepository.save(SavingsProduct.builder()
                .productId(specialSavings.getProductId()).savingType(SavingType.FREE)
                .monthlyPaymentMinAmount(bd("10000")).monthlyPaymentMaxAmount(bd("3000000")).build());

        // SAVINGS 금리
        interestRateRepository.save(ProductInterestRate.builder().productId(diySavings.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(36)
                .minimumJoinAmount(bd("10000")).rate(bd("2.95"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(diySavings.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(1).maximumContractPeriod(36)
                .minimumJoinAmount(bd("10000")).rate(bd("0.60"))
                .conditionDescription("자동이체 설정 우대").effectiveStartDate("20260101").build());

        interestRateRepository.save(ProductInterestRate.builder().productId(dollarSavings.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(6)
                .minimumJoinAmount(bd("10000")).rate(bd("1.00"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(dollarSavings.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(6).maximumContractPeriod(6)
                .minimumJoinAmount(bd("10000")).rate(bd("6.20"))
                .conditionDescription("달러 환전 실적 우대").effectiveStartDate("20260101").build());

        interestRateRepository.save(ProductInterestRate.builder().productId(clearSkySavings.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(36)
                .minimumJoinAmount(bd("10000")).rate(bd("2.85"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(clearSkySavings.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(1).maximumContractPeriod(36)
                .minimumJoinAmount(bd("10000")).rate(bd("1.00"))
                .conditionDescription("맑은하늘 인증코드 등록 우대").effectiveStartDate("20260101").build());

        interestRateRepository.save(ProductInterestRate.builder().productId(militarySavings.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(24).maximumContractPeriod(24)
                .minimumJoinAmount(bd("1000")).rate(bd("5.00"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(militarySavings.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(24).maximumContractPeriod(24)
                .minimumJoinAmount(bd("1000")).rate(bd("5.50"))
                .conditionDescription("정부 기여금 및 납입 완료 우대").effectiveStartDate("20260101").build());

        interestRateRepository.save(ProductInterestRate.builder().productId(specialSavings.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(1).maximumContractPeriod(1)
                .minimumJoinAmount(bd("10000")).rate(bd("2.00"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(specialSavings.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(1).maximumContractPeriod(1)
                .minimumJoinAmount(bd("10000")).rate(bd("4.00"))
                .conditionDescription("특별 조건 충족 우대").effectiveStartDate("20260101").build());

        // SAVINGS 채널
        joinChannelRepository.save(ProductJoinChannel.builder().productId(diySavings.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(diySavings.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(dollarSavings.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(clearSkySavings.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(clearSkySavings.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(militarySavings.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(specialSavings.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());

        // SAVINGS 대상 그룹
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(diySavings.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(dollarSavings.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(clearSkySavings.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(militarySavings.getProductId(), military.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(specialSavings.getProductId(), personal.getTargetGroupId())).build());

        // ══════════════════════════════════════════════════════════════════════
        // SUBSCRIPTION 청약 2개
        // ══════════════════════════════════════════════════════════════════════
        Product housingSubscription = productRepository.save(Product.builder()
                .productType(ProductType.SUBSCRIPTION)
                .productName("주택청약종합저축")
                .description("청약 자격 및 주택마련 저축 상품")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("3.10"))
                .minJoinAmount(bd("20000")).maxJoinAmount(bd("500000"))
                .minPeriodMonth(24).maxPeriodMonth(600)
                .releasedAt("20260101").build());

        Product youthHousing = productRepository.save(Product.builder()
                .productType(ProductType.SUBSCRIPTION)
                .productName("청년 주택드림 청약통장")
                .description("만 19~34세 청년을 위한 우대 청약통장")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("3.10"))
                .minJoinAmount(bd("20000")).maxJoinAmount(bd("1000000"))
                .minPeriodMonth(24).maxPeriodMonth(600)
                .isTaxBenefitAvailable(true)
                .releasedAt("20260101").build());

        subscriptionProductRepository.save(SubscriptionProduct.builder()
                .productId(housingSubscription.getProductId())
                .monthlyPaymentAmount(bd("100000"))
                .minMonthlyPayment(bd("20000")).maxMonthlyPayment(bd("500000"))
                .maxRecognizedPaymentAmount(bd("10000000")).build());
        subscriptionProductRepository.save(SubscriptionProduct.builder()
                .productId(youthHousing.getProductId())
                .monthlyPaymentAmount(bd("200000"))
                .minMonthlyPayment(bd("20000")).maxMonthlyPayment(bd("1000000"))
                .maxRecognizedPaymentAmount(bd("10000000")).build());

        // SUBSCRIPTION 금리
        interestRateRepository.save(ProductInterestRate.builder().productId(housingSubscription.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(24).maximumContractPeriod(600)
                .minimumJoinAmount(bd("20000")).rate(bd("3.10"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthHousing.getProductId())
                .rateType(RateType.BASE).minimumContractPeriod(24).maximumContractPeriod(600)
                .minimumJoinAmount(bd("20000")).rate(bd("3.10"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthHousing.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumContractPeriod(24).maximumContractPeriod(600)
                .minimumJoinAmount(bd("20000")).rate(bd("1.40"))
                .conditionDescription("청년 소득 조건 충족 우대").effectiveStartDate("20260101").build());

        // SUBSCRIPTION 채널
        joinChannelRepository.save(ProductJoinChannel.builder().productId(housingSubscription.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(housingSubscription.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(youthHousing.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());

        // SUBSCRIPTION 대상 그룹
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(housingSubscription.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthHousing.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthHousing.getProductId(), youth.getTargetGroupId())).build());

        // ══════════════════════════════════════════════════════════════════════
        // DEMAND 입출금자유 10개
        // ══════════════════════════════════════════════════════════════════════
        Product ssokMoney = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 쏙머니통장")
                .description("쇼핑용 아껴 쏙머니가 쏙~")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product electionAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("당선통장")
                .description("각종 공직선거 입후보자 및 입후보예정자 선거자금 관리 통장")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product livelihoodAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 생계비계좌")
                .description("생계 유지에 필요한 자금을 최대 250만원까지 보호하는 압류방지 전용통장")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0")).maxJoinAmount(bd("2500000"))
                .releasedAt("20260101").build());
        Product gsPayAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful GS Pay통장")
                .description("GS25와의 만남으로 더 풍성해진 혜택")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product dailyInterest = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("모니모 AXful 매일이자 통장")
                .description("하루만 넣어도 이자가 쌓이는")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.50")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product groupSafe = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 모임금고")
                .description("고인 여유자금을 연 2.0%(최대 1천만원)로 불리는")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("2.00")).minJoinAmount(bd("0")).maxJoinAmount(bd("10000000"))
                .releasedAt("20260101").build());
        Product starAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 스타통장")
                .description("Digital AXful의 대표 통장")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product walletAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 지갑통장")
                .description("일상의 모든 지출을 한 곳에서 관리")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product freeAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 자유입출금통장")
                .description("언제든 자유롭게 입출금 가능한 기본 통장")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.10")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());
        Product youthAccount = productRepository.save(Product.builder()
                .productType(ProductType.DEPOSIT).productName("AXful 청년우대통장")
                .description("만 19~34세 청년을 위한 우대금리 제공")
                .departmentId(productDepartment.getDepartmentId())
                .baseInterestRate(bd("0.50")).minJoinAmount(bd("0"))
                .releasedAt("20260101").build());

        depositProductRepository.save(DepositProduct.builder().productId(ssokMoney.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(electionAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(livelihoodAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(gsPayAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(dailyInterest.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(true).build());
        depositProductRepository.save(DepositProduct.builder().productId(groupSafe.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(starAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(walletAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(freeAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());
        depositProductRepository.save(DepositProduct.builder().productId(youthAccount.getProductId()).depositType(DepositType.DEMAND).isCompoundInterest(false).build());

        // DEMAND 금리 (명시적 금리 있는 상품만)
        interestRateRepository.save(ProductInterestRate.builder().productId(dailyInterest.getProductId())
                .rateType(RateType.BASE).minimumJoinAmount(bd("0")).rate(bd("0.50"))
                .conditionDescription("매일 이자 지급 기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(groupSafe.getProductId())
                .rateType(RateType.BASE).minimumJoinAmount(bd("0")).maximumJoinAmount(bd("10000000")).rate(bd("2.00"))
                .conditionDescription("모임금고 기본금리 (최대 1천만원 한도)").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthAccount.getProductId())
                .rateType(RateType.BASE).minimumJoinAmount(bd("0")).rate(bd("0.50"))
                .conditionDescription("기본금리").effectiveStartDate("20260101").build());
        interestRateRepository.save(ProductInterestRate.builder().productId(youthAccount.getProductId())
                .rateType(RateType.PREFERENTIAL).minimumJoinAmount(bd("0")).rate(bd("0.50"))
                .conditionDescription("청년 우대금리 (+0.5%)").effectiveStartDate("20260101").build());

        // DEMAND 채널
        joinChannelRepository.save(ProductJoinChannel.builder().productId(ssokMoney.getProductId()).joinChannelCode(JoinChannel.BRANCH).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(electionAccount.getProductId()).joinChannelCode(JoinChannel.BRANCH).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(livelihoodAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(gsPayAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(dailyInterest.getProductId()).joinChannelCode(JoinChannel.BRANCH).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(groupSafe.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(starAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(walletAccount.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(walletAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(freeAccount.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(freeAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(youthAccount.getProductId()).joinChannelCode(JoinChannel.WEB).build());
        joinChannelRepository.save(ProductJoinChannel.builder().productId(youthAccount.getProductId()).joinChannelCode(JoinChannel.MOBILE).build());

        // DEMAND 대상 그룹
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(ssokMoney.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(electionAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(livelihoodAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(gsPayAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(dailyInterest.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(groupSafe.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(starAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(walletAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(freeAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthAccount.getProductId(), personal.getTargetGroupId())).build());
        productTargetGroupRepository.save(ProductTargetGroup.builder().id(new ProductTargetGroupId(youthAccount.getProductId(), youth.getTargetGroupId())).build());
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
