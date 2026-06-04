package com.bank.customer.device.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceRequest(
        @NotBlank String deviceTypeCode,
        String deviceName,
        String deviceOsName,
        String deviceOsVersion,
        /** User-Agent 또는 클라이언트가 생성한 기기 식별값 (SHA-256 전 원문) */
        @NotBlank String deviceIdentifier
) {}
