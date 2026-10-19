package com.mailshield.dto;

import java.time.Instant;
import java.util.List;

public record EmailAnalysisResponse(
        Long id,
        Instant timestamp,
        String sender,
        String subject,
        String predictedLabel,
        double spamProbability,
        double hamProbability,
        double logPriorSpam,
        double logPriorHam,
        double logLikelihoodSpam,
        double logLikelihoodHam,
        double totalLogScoreSpam,
        double totalLogScoreHam,
        int totalTokens,
        int recognizedTokens,
        int unknownTokens,
        List<IndicatorToken> topSpamIndicators,
        List<IndicatorToken> topHamIndicators,
        String explanation,
        String disclaimer,
        String inputSource,
        String contentHash,
        int attachmentCount,
        List<String> attachmentNames
) {}
