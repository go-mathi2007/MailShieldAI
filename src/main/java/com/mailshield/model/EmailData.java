package com.mailshield.model;

import java.util.List;

/**
 * Clean extracted representation of an email (from paste or .eml MIME parsing).
 */
public record EmailData(
        String sender,
        String subject,
        String body,
        String date,
        String contentType,
        boolean hasAttachments,
        int attachmentCount,
        List<String> attachmentNames
) {
    public static EmailData fromPasted(String sender, String subject, String body) {
        return new EmailData(
                sender != null ? sender.trim() : "",
                subject != null ? subject.trim() : "",
                body != null ? body.trim() : "",
                null,
                "text/plain",
                false,
                0,
                List.of()
        );
    }
}
