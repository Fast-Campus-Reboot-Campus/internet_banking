package com.bank.customer.cert.service;

import com.bank.common.web.BusinessException;
import com.bank.customer.cert.domain.QrLoginToken;
import com.bank.customer.cert.dto.QrApproveRequest;
import com.bank.customer.cert.dto.QrGenerateResponse;
import com.bank.customer.cert.dto.QrStatusResponse;
import com.bank.customer.cert.repository.QrLoginTokenRepository;
import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.domain.Credential;
import com.bank.customer.customer.repository.CredentialRepository;
import com.bank.customer.customer.repository.CustomerRepository;
import com.bank.customer.login.dto.LoginResponse;
import com.bank.customer.login.service.AuthEventService;
import com.bank.customer.support.CustomerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrLoginService {

    private static final String QR_KEY_PREFIX  = "QR:";
    private static final Duration QR_TOKEN_TTL = Duration.ofMinutes(3);
    private static final Duration QR_JWT_TTL   = Duration.ofMinutes(2);

    private final QrLoginTokenRepository qrLoginTokenRepository;
    private final CredentialRepository credentialRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final AuthEventService authEventService;

    /** PC에서 QR 생성 */
    @Transactional
    public QrGenerateResponse generate(String requestIp) {
        String tokenHash  = UUID.randomUUID().toString().replace("-", "");
        String confirmCode = String.valueOf((int)(Math.random() * 9000) + 1000);
        OffsetDateTime expiry = OffsetDateTime.now().plus(QR_TOKEN_TTL);

        QrLoginToken token = QrLoginToken.builder()
                .qrTokenHash(tokenHash)
                .qrStatusCode(QrLoginToken.STATUS_PENDING)
                .requestIp(requestIp)
                .requestChannelCode("WEB")
                .issuedAt(OffsetDateTime.now())
                .expiryAt(expiry)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        qrLoginTokenRepository.save(token);

        // confirmCode는 Redis에 저장 (tokenHash로 조회)
        redisTemplate.opsForValue().set(QR_KEY_PREFIX + tokenHash + ":code", confirmCode, QR_TOKEN_TTL);

        return new QrGenerateResponse(tokenHash, confirmCode, expiry);
    }

    /** PC 폴링: 상태 조회. APPROVED면 accessToken·refreshToken 포함 반환 */
    @Transactional
    public QrStatusResponse getStatus(String tokenHash) {
        QrLoginToken token = qrLoginTokenRepository.findByQrTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_040));

        if (token.isExpiredByTime() && QrLoginToken.STATUS_PENDING.equals(token.getQrStatusCode())) {
            token.expire();
        }

        if (QrLoginToken.STATUS_APPROVED.equals(token.getQrStatusCode())) {
            String accessToken  = redisTemplate.opsForValue().get(QR_KEY_PREFIX + tokenHash + ":access");
            String refreshToken = redisTemplate.opsForValue().get(QR_KEY_PREFIX + tokenHash + ":refresh");
            return new QrStatusResponse(token.getQrStatusCode(), token.getCustomerId(), accessToken, refreshToken);
        }

        return new QrStatusResponse(token.getQrStatusCode(), null, null, null);
    }

    /**
     * 모바일에서 QR 승인.
     * loginId + password로 사용자를 인증한 뒤 QR 토큰을 APPROVED로 전환하고
     * JWT를 Redis에 저장한다. PC 폴링이 이를 꺼내 로그인에 사용한다.
     *
     * <p>인증 검증만 수행하고 성공/실패 후처리(로그인 시도 이력·토큰·세션·FDS)는
     * {@link AuthEventService} 에 위임한다. QR 승인은 모바일 채널로 기록한다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void approve(QrApproveRequest request, String ip, String userAgent) {
        Long customerId = null;

        try {
            QrLoginToken token = qrLoginTokenRepository.findByQrTokenHash(request.tokenHash())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_040));

            if (token.isExpiredByTime()) {
                token.expire();
                throw new BusinessException(CustomerErrorCode.CUST_041);
            }
            if (!QrLoginToken.STATUS_PENDING.equals(token.getQrStatusCode())
                    && !QrLoginToken.STATUS_SCANNED.equals(token.getQrStatusCode())) {
                throw new BusinessException(CustomerErrorCode.CUST_042);
            }

            // 모바일 사용자 인증
            Credential credential = credentialRepository
                    .findByLoginIdAndDeletedAtIsNull(request.loginId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_010));

            customerId = credential.getCustomerId();

            if (credential.isLocked())           throw new BusinessException(CustomerErrorCode.CUST_011);
            if (!credential.isActive())          throw new BusinessException(CustomerErrorCode.CUST_012);
            if (credential.isPasswordExpired())  throw new BusinessException(CustomerErrorCode.CUST_013);

            if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
                credential.recordLoginFailure();
                throw new BusinessException(
                        credential.isLocked() ? CustomerErrorCode.CUST_011 : CustomerErrorCode.CUST_010);
            }

            Customer customer = customerRepository
                    .findByCustomerIdAndDeletedAtIsNull(credential.getCustomerId())
                    .orElseThrow(() -> new BusinessException(CustomerErrorCode.CUST_002));

            if (!customer.isActive()) throw new BusinessException(CustomerErrorCode.CUST_012);

            credential.recordLoginSuccess();

            // 공통 후처리: 로그인 시도 이력 + 토큰 발급 + 세션 + LOGIN_ATTEMPT FDS(silent)
            LoginResponse tokens = authEventService.onLoginSuccess(
                    customer, request.loginId(), ip, userAgent, AuthEventService.CHANNEL_MOBILE);

            // PC 폴링용 JWT Redis 저장 (2분 TTL)
            redisTemplate.opsForValue().set(QR_KEY_PREFIX + request.tokenHash() + ":access",  tokens.accessToken(),  QR_JWT_TTL);
            redisTemplate.opsForValue().set(QR_KEY_PREFIX + request.tokenHash() + ":refresh", tokens.refreshToken(), QR_JWT_TTL);

            token.approve(customer.getCustomerId());

        } catch (BusinessException e) {
            authEventService.onLoginFailure(request.loginId(), customerId, ip, userAgent,
                    AuthEventService.CHANNEL_MOBILE, e.getErrorCode().getCode(), true);
            throw e;
        }
    }
}
