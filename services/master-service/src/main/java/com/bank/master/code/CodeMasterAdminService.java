package com.bank.master.code;

import com.bank.common.code.CodeDto;
import com.bank.common.code.CodeInvalidationEvent;
import com.bank.common.code.CodeInvalidationSubscriber;
import com.bank.common.code.CodeService;
import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

/**
 * master-service 측 코드 관리(쓰기) + 자체 조회(읽기) 구현.
 *
 * 변경 트랜잭션이 커밋된 뒤에만 Redis pub 으로 무효화 이벤트를 발행하여
 * 캐시 정합성을 보장한다. (롤백 시 무효화 발행 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeMasterAdminService implements CodeService {

    private final CodeMasterRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CodeDto create(String groupCd, String codeCd, String codeName, String codeDesc,
                          Integer sortNo, String activeYn) {
        repository.findByCodeGroupCdAndCodeCdAndDeletedAtIsNull(groupCd, codeCd)
                .ifPresent(c -> { throw new BusinessException(CommonErrorCode.COMMON_409, "이미 존재하는 코드입니다."); });
        CodeMaster saved = repository.save(CodeMaster.builder()
                .codeGroupCd(groupCd)
                .codeCd(codeCd)
                .codeName(codeName)
                .codeDesc(codeDesc)
                .sortNo(sortNo)
                .activeYn(activeYn == null ? "Y" : activeYn)
                .build());
        eventPublisher.publishEvent(CodeInvalidationEvent.of(groupCd, codeCd));
        return toDto(saved);
    }

    @Transactional
    public CodeDto update(Long codeId, String codeName, String codeDesc, Integer sortNo, String activeYn) {
        CodeMaster c = repository.findById(codeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_404, "코드를 찾을 수 없습니다."));
        c.update(codeName, codeDesc, sortNo, activeYn == null ? "Y" : activeYn);
        eventPublisher.publishEvent(CodeInvalidationEvent.of(c.getCodeGroupCd(), c.getCodeCd()));
        return toDto(c);
    }

    @Transactional
    public void delete(Long codeId, Long actorId) {
        CodeMaster c = repository.findById(codeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_404, "코드를 찾을 수 없습니다."));
        c.softDelete(actorId);
        eventPublisher.publishEvent(CodeInvalidationEvent.of(c.getCodeGroupCd(), c.getCodeCd()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CodeDto> find(String groupCd, String codeCd) {
        return repository.findByCodeGroupCdAndCodeCdAndDeletedAtIsNull(groupCd, codeCd).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodeDto> findGroup(String groupCd) {
        return repository.findByCodeGroupCdAndDeletedAtIsNullOrderBySortNoAsc(groupCd)
                .stream().map(this::toDto).toList();
    }

    /** 트랜잭션 커밋 후에만 Redis pub 발행 → 롤백 시 캐시 정합성 보장. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishInvalidation(CodeInvalidationEvent ev) {
        try {
            redisTemplate.convertAndSend(CodeInvalidationSubscriber.CHANNEL, objectMapper.writeValueAsString(ev));
        } catch (JsonProcessingException e) {
            log.warn("코드 무효화 메시지 직렬화 실패", e);
        }
    }

    private CodeDto toDto(CodeMaster c) {
        return new CodeDto(c.getCodeId(), c.getCodeGroupCd(), c.getCodeCd(),
                c.getCodeName(), c.getCodeDesc(), c.getSortNo(), c.getActiveYn());
    }
}
