package com.mailshield.service;

import com.mailshield.model.EmailData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EmlParserServiceTest {

    private EmlParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new EmlParserService();
    }

    @Test
    @DisplayName("Should parse standard plain text .eml message")
    void testParsePlainTextEml() throws Exception {
        String rawEml = """
                From: Security Team <security@example.com>
                To: User <user@example.com>
                Subject: Important Security Notice
                Date: Mon, 19 Oct 2026 10:00:00 +0000
                Content-Type: text/plain; charset=utf-8
                
                Please review your recent account activity and update your credentials immediately.
                """;

        EmailData data = parserService.parseEml(rawEml.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data);
        assertTrue(data.sender().contains("security@example.com"));
        assertEquals("Important Security Notice", data.subject());
        assertTrue(data.body().contains("Please review your recent account activity"));
        assertFalse(data.hasAttachments());
    }

    @Test
    @DisplayName("Should extract text and sanitize HTML-only email without executing scripts")
    void testParseHtmlEml() throws Exception {
        String rawEml = """
                From: Promo <promo@store.com>
                Subject: 50% Off Flash Sale
                Content-Type: text/html; charset=utf-8
                
                <html>
                  <head><script>alert('malicious script');</script></head>
                  <body>
                    <h1>Flash Sale Today!</h1>
                    <p>Exclusive deals on all products. <a href="http://store.com/deal">Shop Now</a></p>
                  </body>
                </html>
                """;

        EmailData data = parserService.parseEml(rawEml.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data);
        assertEquals("50% Off Flash Sale", data.subject());
        assertTrue(data.body().contains("Flash Sale Today!"));
        // Confirm <script> tags were completely stripped by JSoup
        assertFalse(data.body().contains("alert("));
        assertFalse(data.body().contains("<script>"));
    }

    @Test
    @DisplayName("Should identify attachments without reading or executing them")
    void testParseMultipartWithAttachment() throws Exception {
        String rawEml = """
                From: Billing <billing@corp.com>
                Subject: Invoice #10293
                MIME-Version: 1.0
                Content-Type: multipart/mixed; boundary="boundary-xyz"
                
                --boundary-xyz
                Content-Type: text/plain; charset=utf-8
                
                Please find your invoice attached for last month.
                
                --boundary-xyz
                Content-Type: application/pdf; name="invoice_10293.pdf"
                Content-Disposition: attachment; filename="invoice_10293.pdf"
                
                JVBERi0xLjQKJcTl8uXr...
                --boundary-xyz--
                """;

        EmailData data = parserService.parseEml(rawEml.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data);
        assertTrue(data.hasAttachments());
        assertEquals(1, data.attachmentCount());
        assertTrue(data.attachmentNames().contains("invoice_10293.pdf"));
        assertTrue(data.body().contains("Please find your invoice attached"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on empty byte array")
    void testEmptyEmlThrows() {
        assertThrows(IllegalArgumentException.class, () -> parserService.parseEml(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> parserService.parseEml(null));
    }
}
