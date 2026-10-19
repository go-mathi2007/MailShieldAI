package com.mailshield.dto;

public record ScanStatisticsResponse(
        long totalScans,
        long spamCount,
        long hamCount,
        double spamPercentage,
        double hamPercentage
) {}
