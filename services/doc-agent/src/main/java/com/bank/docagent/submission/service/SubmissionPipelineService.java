package com.bank.docagent.submission.service;

import com.bank.docagent.kafka.SubmissionEventProducer;
import com.bank.docagent.submission.domain.DocumentSubmission;
import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.bank.docagent.submission.dto.ExtractionResult;
import com.bank.docagent.submission.event.ExtractionCompletedEvent;
import com.bank.docagent.submission.service.DocumentClassifyService.DocType;
import com.bank.docagent.submission.service.OcrMaskingService.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * L1 → L2 → L3 파이프라인 오케스트레이터.
 * 각 단계는 Spring 레이어에서 처리, Python 사이드카 호출은 L3에서만 발생.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionPipelineService {

    private final DocumentIngestService ingestService;
    private final DocumentClassifyService classifyService;
    private final OcrMaskingService ocrMaskingService;
    private final SubmissionEventProducer eventProducer;

    public ExtractionResult process(String applicationId, String docCode,
                                    MultipartFile file) throws IOException {
        // L1: Ingest
        byte[] bytes = file.getBytes();
        DocumentSubmission submission = ingestService.ingest(applicationId, docCode, file);

        // L2: Classify (OCR 없이 파일명·docCode 힌트 먼저, OCR 결과로 재확인은 L3 후)
        DocType docType = DocType.UNKNOWN;

        // L3: OCR + Masking
        OcrResult ocrResult = ocrMaskingService.extractAndMask(
            submission, bytes, file.getContentType(), applicationId);

        // L3 결과로 분류 보정
        docType = classifyService.classify(ocrResult.rawText());

        VerifyStatus status = ocrResult.regions().isEmpty()
            ? VerifyStatus.NEEDS_RESUBMIT   // OCR 결과 없음 → 재제출
            : VerifyStatus.AUTO_PASS;        // 정상 추출 (D-5에서 Verify 단계로 갱신)

        submission.updateStatus(status);

        // Kafka 이벤트 발행
        ExtractionCompletedEvent event = ExtractionCompletedEvent.of(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), status, ocrResult.regions(),
            submission.getMaskedObjectKey()
        );
        eventProducer.publishExtracted(event);

        log.info("파이프라인 완료: submissionId={} docType={} status={}",
            submission.getSubmissionId(), docType, status);

        return ExtractionResult.skeleton(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), ocrResult.regions(), ocrResult.maskedText(), status
        );
    }
}
