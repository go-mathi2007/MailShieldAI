package com.mailshield.dto;

/**
 * Represents a token and its statistical contribution to classification.
 */
public record IndicatorToken(
        String token,
        int countInDoc,
        double spamLikelihood,
        double hamLikelihood,
        double logLikelihoodRatio,
        String category
) {}
