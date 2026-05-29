package com.bank.loan.document.docagent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * DocAgentClient 단위 테스트 — Spring 컨텍스트 없이 MockRestServiceServer 로 doc-agent 응답 stub.
 *
 * 검증 포인트:
 *   1) AUTO_PASS 응답 → SubmissionResult 정확 매핑
 *   2) NEEDS_RESUBMIT 응답 → verifyStatus 매핑
 *   3) HOLD 응답 → verifyStatus 매핑
 *   4) 5xx 재시도 3회 후 DocAgentException
 *   5) 4xx 즉시 DocAgentException (재시도 없음)
 *   6) 5xx 2회 후 3회차 성공
 */
class DocAgentClientTest {

    private static final String BASE_URL       = "http://doc-agent:8080";
    private static final String SUBMIT_PATH    = "/api/documents/submit";
    private static final String APPLICATION_ID = "LOAN-2026-001";
    private static final String DOC_CODE       = "EMPLOYMENT_CERT";
    private static final String PRODUCT_ID     = "1";

    private MockRestServiceServer server;
    private DocAgentClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient.Builder builder = RestClient.builder(restTemplate);
        DocAgentProperties props = new DocAgentProperties(BASE_URL, 0, 0);
        client = new DocAgentClient(builder, props);
    }

    @Test
    void AUTO_PASS_응답_매핑() {
        server.expect(requestTo(BASE_URL + SUBMIT_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "submission_id": "sub-001",
                          "application_id": "LOAN-2026-001",
                          "doc_code": "EMPLOYMENT_CERT",
                          "verify_status": "AUTO_PASS",
                          "document_verification": {
                            "status": "AUTO_PASS",
                            "confidence_score": 0.95
                          },
                          "pipeline_stage": "L5_VERIFY_COMPLETE"
                        }
                        """, MediaType.APPLICATION_JSON));

        SubmissionResult result = client.submit(APPLICATION_ID, DOC_CODE, PRODUCT_ID, mockFile());

        assertThat(result.submissionId()).isEqualTo("sub-001");
        assertThat(result.verifyStatus()).isEqualTo(SubmissionResult.VERIFY_AUTO_PASS);
        assertThat(result.documentVerification().confidenceScore()).isEqualTo(0.95);
        server.verify();
    }

    @Test
    void NEEDS_RESUBMIT_응답_매핑() {
        server.expect(requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withSuccess("""
                        {
                          "submission_id": "sub-002",
                          "application_id": "LOAN-2026-001",
                          "doc_code": "EMPLOYMENT_CERT",
                          "verify_status": "NEEDS_RESUBMIT",
                          "document_verification": { "confidence_score": 0.3 }
                        }
                        """, MediaType.APPLICATION_JSON));

        SubmissionResult result = client.submit(APPLICATION_ID, DOC_CODE, PRODUCT_ID, mockFile());

        assertThat(result.verifyStatus()).isEqualTo(SubmissionResult.VERIFY_NEEDS_RESUBMIT);
        assertThat(result.submissionId()).isEqualTo("sub-002");
        server.verify();
    }

    @Test
    void HOLD_응답_매핑() {
        server.expect(requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withSuccess("""
                        {
                          "submission_id": "sub-003",
                          "application_id": "LOAN-2026-001",
                          "doc_code": "ID_CARD",
                          "verify_status": "HOLD",
                          "document_verification": { "confidence_score": 0.6 }
                        }
                        """, MediaType.APPLICATION_JSON));

        SubmissionResult result = client.submit(APPLICATION_ID, "ID_CARD", PRODUCT_ID, mockFile());

        assertThat(result.verifyStatus()).isEqualTo(SubmissionResult.VERIFY_HOLD);
        server.verify();
    }

    @Test
    void 서버_5xx_재시도_3회_후_DocAgentException() {
        server.expect(ExpectedCount.times(3), requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.submit(APPLICATION_ID, DOC_CODE, PRODUCT_ID, mockFile()))
                .isInstanceOf(DocAgentException.class)
                .hasMessageContaining("3 attempts");
        server.verify();
    }

    @Test
    void 서버_4xx_재시도_없이_즉시_DocAgentException() {
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.submit(APPLICATION_ID, DOC_CODE, PRODUCT_ID, mockFile()))
                .isInstanceOf(DocAgentException.class)
                .hasMessageContaining("4xx");
        server.verify();
    }

    @Test
    void 서버_5xx_2회_후_3회차_성공() {
        server.expect(ExpectedCount.times(2), requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withServerError());
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + SUBMIT_PATH))
                .andRespond(withSuccess("""
                        {
                          "submission_id": "sub-004",
                          "application_id": "LOAN-2026-001",
                          "doc_code": "EMPLOYMENT_CERT",
                          "verify_status": "AUTO_PASS",
                          "document_verification": { "confidence_score": 0.9 }
                        }
                        """, MediaType.APPLICATION_JSON));

        SubmissionResult result = client.submit(APPLICATION_ID, DOC_CODE, PRODUCT_ID, mockFile());

        assertThat(result.verifyStatus()).isEqualTo(SubmissionResult.VERIFY_AUTO_PASS);
        server.verify();
    }

    private MockMultipartFile mockFile() {
        return new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
    }
}
