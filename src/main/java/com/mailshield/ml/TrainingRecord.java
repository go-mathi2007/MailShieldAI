package com.mailshield.ml;

import java.util.Objects;

/**
 * Immutable record representing a labelled email for model training or evaluation.
 */
public record TrainingRecord(String label, String text) {

    public static final String LABEL_SPAM = "SPAM";
    public static final String LABEL_HAM = "HAM";

    public TrainingRecord {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(text, "text must not be null");
        label = label.trim().toUpperCase();
        if (!label.equals(LABEL_SPAM) && !label.equals(LABEL_HAM)) {
            throw new IllegalArgumentException("Invalid label: " + label + ". Expected SPAM or HAM.");
        }
    }

    public boolean isSpam() {
        return LABEL_SPAM.equalsIgnoreCase(label);
    }

    public boolean isHam() {
        return LABEL_HAM.equalsIgnoreCase(label);
    }
}
