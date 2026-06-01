package com.bank.loan.payment.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentServiceProperties.class)
public class PaymentServiceConfig {}
