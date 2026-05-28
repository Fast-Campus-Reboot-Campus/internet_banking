package com.bank.docagent.submission.service;

import com.bank.docagent.kafka.SubmissionEventProducer;
import com.bank.docagent.submission.domain.DocumentSubmission;
import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.bank.docagent.submission.dto.ExtractionResult;
import com.bank.docagent.submission.dto.extracted.StructuredData;
import com.bank.docagent.submission.event.ExtractionCompletedEvent;
import com.bank.docagent.submission.service.DocumentClassifyService.DocType;
import com.bank.docagent.submission.service.OcrMaskingService.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * L1 → L2 → L3 → L4 파이프라인 오케스트레이터.
 * Python 사이드카 호출: L3(OCR), L4(LLM 구조화 추출)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionPipelineService {

    private final DocumentIngestService ingestService;
    private final DocumentClassifyService classifyService;
    private final OcrMaskingService ocrMaskingService;
    private final StructuredExtractService extractService;
    private final SubmissionEventProducer eventProducer;

    public ExtractionResult process(String applicationId, String docCode,
                                    MultipartFile file) throws IOException {
        // L1: Ingest — 포맷 검증, MinIO 원본 저장
        byte[] bytes = file.getBytes();
        DocumentSubmission submission = ingestService.ingest(applicationId, docCode, file);

        // L3: OCR + Masking (사이드카 호출)
        OcrResult ocrResult = ocrMaskingService.extractAndMask(
            submission, bytes, file.getContentType(), applicationId);

        // L2: OCR 결과 텍스트로 서류 유형 분류
        DocType docType = classifyService.classify(ocrResult.rawText());

        // L4: LLM 구조화 추출 (사이드카 호출, fallback → 빈 필드)
        StructuredData structuredData = extractService.extract(
            submission.getSubmissionId().toString(), docType, ocrResult.maskedText());

        VerifyStatus status = ocrResult.regions().isEmpty()
            ? VerifyStatus.NEEDS_RESUBMIT
            : VerifyStatus.AUTO_PASS;   // D-5 Verify 단계에서 최종 결정

        submission.updateStatus(status);

        ExtractionCompletedEvent event = ExtractionCompletedEvent.of(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), status, ocrResult.regions(),
            submission.getMaskedObjectKey()
        );
        eventProducer.publishExtracted(event);

        log.info("파이프라인 완료: submissionId={} docType={} status={}",
            submission.getSubmissionId(), docType, status);

        return ExtractionResult.of(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), ocrResult.regions(), ocrResult.maskedText(),
            structuredData, status
        );
    }
}
