package com.bank.common.code;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.ObjectProvider;

public class ValidCodeValidator implements ConstraintValidator<ValidCode, String> {

    private final ObjectProvider<CodeService> codeServiceProvider;
    private String group;
    private boolean allowNull;

    public ValidCodeValidator(ObjectProvider<CodeService> codeServiceProvider) {
        this.codeServiceProvider = codeServiceProvider;
    }

    @Override
    public void initialize(ValidCode anno) {
        this.group = anno.group();
        this.allowNull = anno.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return allowNull;
        CodeService cs = codeServiceProvider.getIfAvailable();
        if (cs == null) return true; // 코드 서비스 미주입 환경(테스트 등) 은 통과
        return cs.exists(group, value);
    }
}
