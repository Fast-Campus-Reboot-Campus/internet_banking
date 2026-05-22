package com.bank.ai.rag.scheduler;

import com.bank.ai.rag.admin.RagDocumentAdminService;
import com.bank.ai.rag.document.domain.RagDocument;
import com.bank.ai.rag.document.repository.RagDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RagIngestionScheduler 단위 테스트.
 *
 * 검증 포인트:
 *   1) bootstrap → pending 조회 → 각 문서 reindex 순서
 *   2) pending 0건이면 reindex 호출 없음
 *   3) reindex 한 건 실패해도 나머지 문서 진행
 *   4) bootstrap 자체 실패해도 pending 인제스트는 시도
 */
class RagIngestionSchedulerTest {

    private RagDocumentAdminService adminService;
    private RagDocumentRepository    documentRepository;
    private RagIngestionScheduler    scheduler;

    @BeforeEach
    void setUp() {
        adminService       = mock(RagDocumentAdminService.class);
        documentRepository = mock(RagDocumentRepository.class);
        scheduler = new RagIngestionScheduler(adminService, documentRepository);
    }

    @Test
    void run_은_bootstrap_후_미인제스트_문서를_reindex() throws IOException {
        // doc() 안의 when()/thenReturn() 이 외부 stubbing 의 thenReturn 인자 평가 사이클과
        // 겹치면 Mockito UnfinishedStubbingException 이 난다 — mock 을 먼저 만들고 변수에 담아야 함.
        RagDocument d1 = doc(10L);
        RagDocument d2 = doc(20L);
        RagDocument d3 = doc(30L);
        when(adminService.bootstrapFromCsv()).thenReturn(2);
        when(documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of(d1, d2, d3));

        scheduler.run();

        InOrder order = inOrder(adminService, documentRepository);
        order.verify(adminService).bootstrapFromCsv();
        order.verify(documentRepository).findAllByIngestedAtIsNullAndDeletedAtIsNull();
        verify(adminService).reindex(10L);
        verify(adminService).reindex(20L);
        verify(adminService).reindex(30L);
    }

    @Test
    void run_은_pending_없으면_reindex_호출_없음() throws IOException {
        when(adminService.bootstrapFromCsv()).thenReturn(0);
        when(documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of());

        scheduler.run();

        verify(adminService).bootstrapFromCsv();
        verify(adminService, never()).reindex(anyLong());
    }

    @Test
    void reindex_한건_실패해도_나머지_진행() throws IOException {
        RagDocument d1 = doc(1L);
        RagDocument d2 = doc(2L);
        RagDocument d3 = doc(3L);
        when(adminService.bootstrapFromCsv()).thenReturn(0);
        when(documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of(d1, d2, d3));
        doThrow(new RuntimeException("parse fail")).when(adminService).reindex(2L);

        scheduler.run();

        verify(adminService).reindex(1L);
        verify(adminService).reindex(2L);
        verify(adminService).reindex(3L);   // 가운데 실패해도 마지막까지 시도
    }

    @Test
    void bootstrap_실패해도_pending_인제스트는_시도() throws IOException {
        RagDocument d7 = doc(7L);
        when(adminService.bootstrapFromCsv()).thenThrow(new RuntimeException("io fail"));
        when(documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of(d7));

        scheduler.run();

        verify(adminService).bootstrapFromCsv();
        verify(adminService).reindex(7L);
    }

    @Test
    void 모든_reindex_실패해도_run_정상_종료() throws IOException {
        RagDocument d1 = doc(1L);
        RagDocument d2 = doc(2L);
        when(adminService.bootstrapFromCsv()).thenReturn(0);
        when(documentRepository.findAllByIngestedAtIsNullAndDeletedAtIsNull())
                .thenReturn(List.of(d1, d2));
        doThrow(new RuntimeException("fail")).when(adminService).reindex(anyLong());

        scheduler.run();   // 예외 전파 없어야 함

        verify(adminService, times(2)).reindex(anyLong());
    }

    /** RagDocument 는 protected 생성자 + IDENTITY PK 라 직접 인스턴스화 어려움 → mock 으로 docId 만 stub. */
    private static RagDocument doc(long docId) {
        RagDocument d = mock(RagDocument.class);
        when(d.getDocId()).thenReturn(docId);
        return d;
    }
}
