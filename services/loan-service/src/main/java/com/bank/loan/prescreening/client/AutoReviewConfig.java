package com.bank.loan.prescreening.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AutoReviewProperties.class)
public class AutoReviewConfig {}
