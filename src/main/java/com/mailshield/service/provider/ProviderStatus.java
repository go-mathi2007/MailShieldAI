package com.mailshield.service.provider;

public record ProviderStatus(
        String providerName,
        boolean isConfigured,
        String statusMessage,
        String authType
) {}
