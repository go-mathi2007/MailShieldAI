package com.mailshield.service.provider;

/**
 * Interface contract for external email providers (Gmail, Outlook, IMAP).
 * Prepared for future OAuth2 token exchange without storing user credentials.
 */
public interface EmailProviderService {

    String getProviderName();

    ProviderStatus getStatus();
}
