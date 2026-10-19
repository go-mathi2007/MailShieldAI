package com.mailshield.ml;

/**
 * Encapsulates the results of evaluating the Naive Bayes classifier on a held-out test set.
 */
public record EvaluationResult(
        int totalDatasetSize,
        int trainSetSize,
        int testSetSize,
        int trainSpamCount,
        int trainHamCount,
        int testSpamCount,
        int testHamCount,
        double accuracy,
        double precision,
        double recall,
        double f1Score,
        ConfusionMatrix confusionMatrix,
        boolean isSmallDataset,
        String evaluationNote
) {}
