package com.bank.docagent.submission.service;

import com.bank.docagent.forgery.service.ForgeryAnalysisService;
import com.bank.docagent.forgery.service.ForgeryAnalysisService.ForgeryResult;
import com.bank.docagent.kafka.SubmissionEventProducer;
import com.bank.docagent.submission.domain.DocumentSubmission;
import com.bank.docagent.submission.domain.DocumentSubmission.VerifyStatus;
import com.bank.docagent.submission.dto.ExtractionResult;
import com.bank.docagent.submission.dto.extracted.StructuredData;
import com.bank.docagent.submission.dto.verification.VerificationBlock;
import com.bank.docagent.submission.event.ExtractionCompletedEvent;
import com.bank.docagent.submission.service.DocumentClassifyService.DocType;
import com.bank.docagent.submission.service.OcrMaskingService.OcrResult;
import com.bank.docagent.verify.service.DocumentVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * L1 → L4b(Forgery) → L3 → L2 → L4 → L5 파이프라인 오케스트레이터.
 * Python 사이드카 호출: L4b(위변조), L3(OCR), L4(LLM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionPipelineService {

    private final DocumentIngestService    ingestService;
    private final DocumentClassifyService  classifyService;
    private final OcrMaskingService        ocrMaskingService;
    private final StructuredExtractService extractService;
    private final ForgeryAnalysisService   forgeryService;
    private final DocumentVerifyService    verifyService;
    private final SubmissionEventProducer  eventProducer;

    @Value("${doc-agent.default-product-id:P001}")
    private String defaultProductId;

    public ExtractionResult process(String applicationId, String docCode,
                                    MultipartFile file) throws IOException {
        return process(applicationId, docCode, defaultProductId, file);
    }

    public ExtractionResult process(String applicationId, String docCode,
                                    String productId, MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        // L1: Ingest — 포맷 검증, MinIO 원본 저장
        DocumentSubmission submission = ingestService.ingest(applicationId, docCode, file);

        // L4b: 위변조 시그널 분석 (사이드카, raw bytes 사용)
        ForgeryResult forgeryResult = forgeryService.analyze(
            submission.getSubmissionId(), docCode, bytes, file.getContentType());

        // L3: OCR + Masking (사이드카)
        OcrResult ocrResult = ocrMaskingService.extractAndMask(
            submission, bytes, file.getContentType(), applicationId);

        // L2: OCR 텍스트 기반 서류 유형 분류
        DocType docType = classifyService.classify(ocrResult.rawText());

        // L4: LLM 구조화 추출 (사이드카)
        StructuredData structuredData = extractService.extract(
            submission.getSubmissionId().toString(), docType, ocrResult.maskedText());

        // L5: 룰 검증 + 진위확인 + 위변조 점수 합산
        VerificationBlock verification = verifyService.verify(
            submission, docType, structuredData, productId,
            forgeryResult.aggregateScore(), forgeryResult.signals());

        VerifyStatus finalStatus = verification.status();
        submission.updateStatus(finalStatus);

        // HOLD 시 humanReviewStatus PENDING 세팅
        if (finalStatus == VerifyStatus.HOLD) {
            submission.markHoldPending();
        }

        ExtractionCompletedEvent event = ExtractionCompletedEvent.of(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), finalStatus, ocrResult.regions(),
            submission.getMaskedObjectKey()
        );
        eventProducer.publishExtracted(event);

        log.info("파이프라인 완료: submissionId={} docType={} forgeryScore={} status={}",
            submission.getSubmissionId(), docType,
            forgeryResult.aggregateScore(), finalStatus);

        return ExtractionResult.of(
            submission.getSubmissionId(), applicationId, docCode,
            docType.name(), ocrResult.regions(), ocrResult.maskedText(),
            structuredData, verification, finalStatus
        );
    }
}
