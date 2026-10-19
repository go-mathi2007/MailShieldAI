package com.mailshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailAnalysisRequest {

    @Size(max = 255, message = "Sender address must not exceed 255 characters")
    private String sender;

    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @NotBlank(message = "Email body must not be blank")
    @Size(max = 100000, message = "Email body exceeds maximum allowed length of 100,000 characters")
    private String body;

    public EmailAnalysisRequest() {}

    public EmailAnalysisRequest(String sender, String subject, String body) {
        this.sender = sender;
        this.subject = subject;
        this.body = body;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
