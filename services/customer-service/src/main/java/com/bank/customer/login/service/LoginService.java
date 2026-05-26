package com.bank.customer.login.service;

import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.web.BusinessException;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.login.dto.LoginRequest;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final CredentialRepository credentialRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * noRollbackFor: 비밀번호 실패 카운트·잠금 상태는 예외 발생 시에도 반드시 커밋돼야 한다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {

        Credential credential = credentialRepository
                .findByLoginIdAndDeletedAtIsNull(request.loginId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));

        if (credential.isLocked()) {
            throw new BusinessException(CustomerErrorCode.CUST_011);
        }
        if (!credential.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }
        if (credential.isPasswordExpired()) {
            throw new BusinessException(CustomerErrorCode.CUST_013);
        }

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            credential.recordLoginFailure();
            // 임계치 도달로 잠금 전환된 경우 잠금 오류 코드로 응답
            throw new BusinessException(
                    credential.isLocked() ? CustomerErrorCode.CUST_011 : CustomerErrorCode.CUST_010);
        }

        Customer customer = customerRepository
                .findByCustomerIdAndDeletedAtIsNull(credential.getCustomerId())
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

        if (!customer.isActive()) {
            throw new BusinessException(CustomerErrorCode.CUST_012);
        }

        credential.recordLoginSuccess();

        String accessToken  = jwtProvider.generateAccessToken(
                customer.getCustomerId(), customer.getEmail(), List.of("ROLE_CUSTOMER"));
        String refreshToken = jwtProvider.generateRefreshToken(customer.getCustomerId());

        return new LoginResponse(customer.getCustomerId(), accessToken, refreshToken);
    }
}
