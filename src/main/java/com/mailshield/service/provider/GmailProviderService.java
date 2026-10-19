package com.mailshield.service.provider;

import org.springframework.stereotype.Service;

@Service
public class GmailProviderService implements EmailProviderService {

    @Override
    public String getProviderName() {
        return "Gmail (Google Workspace)";
    }

    @Override
    public ProviderStatus getStatus() {
        return new ProviderStatus(
                getProviderName(),
                false,
                "Gmail OAuth2 Integration is coming soon. Direct API integration is optional; MailShield AI works 100% locally with pasted messages and .eml files.",
                "OAuth 2.0 (PKCE)"
        );
    }
}
