package com.mailshield.dto;

import java.time.Instant;

public record HistoryItemResponse(
        Long id,
        Instant timestamp,
        String subject,
        String sender,
        String predictedLabel,
        double spamProbability,
        double hamProbability,
        String contentHash,
        String inputSource,
        String snippet,
        String explanation,
        int attachmentCount
) {}
