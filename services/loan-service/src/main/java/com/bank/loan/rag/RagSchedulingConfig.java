package com.bank.loan.rag;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RAG 관련 스케줄링 + ConfigurationProperties 등록 — D3-2.
 */
@Configuration
@EnableScheduling
@ConfigurationPropertiesScan("com.bank.loan.rag")
public class RagSchedulingConfig {}
