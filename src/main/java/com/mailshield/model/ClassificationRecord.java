package com.mailshield.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for recording email scan results.
 * Strictly adheres to privacy guidelines: full email bodies are NEVER persisted.
 */
@Entity
@Table(name = "classification_history")
public class ClassificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 500)
    private String subject;

    @Column(length = 255)
    private String sender;

    @Column(nullable = false, length = 10)
    private String predictedLabel;

    @Column(nullable = false)
    private Double spamProbability;

    @Column(nullable = false)
    private Double hamProbability;

    @Column(length = 64)
    private String contentHash;

    @Column(length = 20)
    private String inputSource;

    @Column(length = 200)
    private String snippet;

    @Column(length = 500)
    private String explanation;

    private Integer attachmentCount = 0;

    public ClassificationRecord() {
    }

    public ClassificationRecord(
            Instant timestamp,
            String subject,
            String sender,
            String predictedLabel,
            Double spamProbability,
            Double hamProbability,
            String contentHash,
            String inputSource,
            String snippet,
            String explanation,
            Integer attachmentCount
    ) {
        this.timestamp = timestamp;
        this.subject = subject;
        this.sender = sender;
        this.predictedLabel = predictedLabel;
        this.spamProbability = spamProbability;
        this.hamProbability = hamProbability;
        this.contentHash = contentHash;
        this.inputSource = inputSource;
        this.snippet = snippet;
        this.explanation = explanation;
        this.attachmentCount = attachmentCount != null ? attachmentCount : 0;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getPredictedLabel() {
        return predictedLabel;
    }

    public void setPredictedLabel(String predictedLabel) {
        this.predictedLabel = predictedLabel;
    }

    public Double getSpamProbability() {
        return spamProbability;
    }

    public void setSpamProbability(Double spamProbability) {
        this.spamProbability = spamProbability;
    }

    public Double getHamProbability() {
        return hamProbability;
    }

    public void setHamProbability(Double hamProbability) {
        this.hamProbability = hamProbability;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getInputSource() {
        return inputSource;
    }

    public void setInputSource(String inputSource) {
        this.inputSource = inputSource;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
}
