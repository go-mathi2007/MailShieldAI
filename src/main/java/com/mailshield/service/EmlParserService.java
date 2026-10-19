package com.mailshield.service;

import com.mailshield.model.EmailData;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service for parsing MIME .eml email files into clean, safe text data.
 * Adheres to strict cybersecurity rules:
 * - Does NOT save or execute attachments.
 * - Sanitizes HTML via JSoup to prevent XSS / script execution.
 * - Extracts sender, subject, date, and headers.
 */
@Service
public class EmlParserService {

    private static final Logger log = LoggerFactory.getLogger(EmlParserService.class);

    /**
     * Parses an EML file stream into EmailData.
     *
     * @param emlBytes raw bytes of the EML file
     * @return EmailData
     * @throws MessagingException if MIME parsing fails
     */
    public EmailData parseEml(byte[] emlBytes) throws Exception {
        if (emlBytes == null || emlBytes.length == 0) {
            throw new IllegalArgumentException("Uploaded EML file is empty.");
        }

        Session session = Session.getDefaultInstance(new Properties(), null);
        try (InputStream is = new ByteArrayInputStream(emlBytes)) {
            MimeMessage message = new MimeMessage(session, is);

            String subject = message.getSubject() != null ? message.getSubject().trim() : "";

            String sender = "";
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                sender = fromAddresses[0].toString().trim();
            }

            String date = message.getSentDate() != null ? message.getSentDate().toString() : "";
            String contentType = message.getContentType() != null ? message.getContentType() : "text/plain";

            StringBuilder plainTextBody = new StringBuilder();
            StringBuilder htmlBody = new StringBuilder();
            List<String> attachmentNames = new ArrayList<>();

            extractContent(message, plainTextBody, htmlBody, attachmentNames);

            String finalBody;
            if (plainTextBody.length() > 0) {
                finalBody = plainTextBody.toString().trim();
            } else if (htmlBody.length() > 0) {
                // Safely extract plain text from HTML, removing all script, style, and tag elements
                finalBody = Jsoup.parse(htmlBody.toString()).text().trim();
            } else {
                finalBody = "";
            }

            return new EmailData(
                    sender,
                    subject,
                    finalBody,
                    date,
                    contentType,
                    !attachmentNames.isEmpty(),
                    attachmentNames.size(),
                    attachmentNames
            );
        } catch (Exception e) {
            log.error("Failed to parse .eml MIME file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Unable to parse email file. Please verify it is a valid RFC-822 / .eml message. Error: " + e.getMessage());
        }
    }

    private void extractContent(Part part, StringBuilder plainText, StringBuilder htmlText, List<String> attachments)
            throws Exception {

        String disposition = part.getDisposition();
        String fileName = part.getFileName();

        // Check if part is an attachment
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || (fileName != null && !fileName.isBlank())) {
            attachments.add(fileName != null ? sanitizeHeader(fileName) : "unnamed_attachment");
            return; // Never read or execute attachment content
        }

        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            if (content instanceof String s) {
                if (plainText.length() > 0) plainText.append("\n\n");
                plainText.append(s);
            }
        } else if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            if (content instanceof String s) {
                if (htmlText.length() > 0) htmlText.append("\n\n");
                htmlText.append(s);
            }
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                extractContent(multipart.getBodyPart(i), plainText, htmlText, attachments);
            }
        }
    }

    private String sanitizeHeader(String val) {
        if (val == null) return "";
        return val.replaceAll("[\\r\\n]", "").trim();
    }
}
