package com.bank.deposit.service;

import com.bank.deposit.domain.entity.JoinMethod;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.exception.ErrorCode;
import com.bank.deposit.repository.JoinMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JoinMethodService {

    private final JoinMethodRepository joinMethodRepository;

    public List<JoinMethod> findAll() {
        return joinMethodRepository.findAll();
    }

    @Transactional
    public JoinMethod create(String joinMethodName, String description) {
        return joinMethodRepository.save(JoinMethod.builder()
                .joinMethodName(joinMethodName)
                .description(description)
                .build());
    }
}
