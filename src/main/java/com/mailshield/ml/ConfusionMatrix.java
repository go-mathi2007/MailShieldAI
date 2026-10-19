package com.mailshield.ml;

/**
 * 2x2 Confusion Matrix for binary spam classification.
 * Positive Class = SPAM, Negative Class = HAM.
 */
public record ConfusionMatrix(
        int truePositives,   // Actual SPAM, Predicted SPAM
        int falsePositives,  // Actual HAM,  Predicted SPAM
        int trueNegatives,   // Actual HAM,  Predicted HAM
        int falseNegatives   // Actual SPAM, Predicted HAM
) {
    public int total() {
        return truePositives + falsePositives + trueNegatives + falseNegatives;
    }
}
