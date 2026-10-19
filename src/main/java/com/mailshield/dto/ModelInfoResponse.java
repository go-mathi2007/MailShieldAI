package com.mailshield.dto;

import com.mailshield.ml.EvaluationResult;

public record ModelInfoResponse(
        boolean isTrained,
        String algorithm,
        double alpha,
        int totalTrainingDocuments,
        int spamTrainingDocuments,
        int hamTrainingDocuments,
        int vocabularySize,
        double priorSpam,
        double priorHam,
        EvaluationResult evaluation
) {}
