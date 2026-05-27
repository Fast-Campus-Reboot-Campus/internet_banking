package com.bank.common.code;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DTO 필드의 코드값이 CODE_MASTER 의 지정 그룹에 속하는지 검증.
 *
 *   @ValidCode(group = "LOAN_STATUS") String statusCd;
 *
 * CodeService 빈이 등록되지 않은 환경에서는 검증을 통과시킨다.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCodeValidator.class)
public @interface ValidCode {

    String group();

    boolean allowNull() default true;

    String message() default "유효하지 않은 코드값입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
