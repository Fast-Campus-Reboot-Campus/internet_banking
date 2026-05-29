package com.bank.loan.document.docagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocAgentClient {

    private static final Logger log = LoggerFactory.getLogger(DocAgentClient.class);
    private static final String SUBMIT_PATH  = "/api/documents/submit";
    private static final int    MAX_ATTEMPTS = 3;
    private static final long   BACKOFF_MS   = 200L;

    private final RestClient restClient;

    public DocAgentClient(RestClient.Builder builder, DocAgentProperties props) {
        RestClient.Builder b;
        if (props.connectTimeoutMs() > 0 || props.readTimeoutMs() > 0) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (props.connectTimeoutMs() > 0) factory.setConnectTimeout(props.connectTimeoutMs());
            if (props.readTimeoutMs()    > 0) factory.setReadTimeout(props.readTimeoutMs());
            b = builder.requestFactory(factory).baseUrl(props.baseUrl());
        } else {
            b = builder.baseUrl(props.baseUrl());
        }
        this.restClient = b.build();
    }

    public SubmissionResult submit(String applicationId, String docCode, String productId,
                                   MultipartFile file) {
        Throwable lastEx = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SubmissionResult result = restClient.post()
                        .uri(SUBMIT_PATH)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(buildBody(applicationId, docCode, productId, file))
                        .retrieve()
                        .body(SubmissionResult.class);
                if (result == null) throw new DocAgentException("empty response from doc-agent", null);
                return result;
            } catch (HttpClientErrorException e) {
                throw new DocAgentException("doc-agent 4xx: " + e.getStatusCode(), e);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                lastEx = e;
                log.warn("doc-agent attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.toString());
                if (attempt < MAX_ATTEMPTS) sleepBackoff(attempt);
            }
        }
        throw new DocAgentException("doc-agent failed after " + MAX_ATTEMPTS + " attempts", lastEx);
    }

    private MultiValueMap<String, Object> buildBody(String applicationId, String docCode,
                                                     String productId, MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("applicationId", applicationId);
        body.add("docCode", docCode);
        if (productId != null) body.add("productId", productId);
        body.add("file", file.getResource());
        return body;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MS * (1L << (attempt - 1)));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DocAgentException("retry interrupted", ie);
        }
    }
}
