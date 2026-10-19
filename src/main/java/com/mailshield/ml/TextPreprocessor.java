package com.mailshield.ml;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text Preprocessor for MailShield AI.
 * Handles tokenization, case normalization, URL and email recognition,
 * numerical feature abstraction, and punctuation handling.
 */
@Component
public class TextPreprocessor {

    // Regex patterns for entity abstraction
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
            "([\\$€£¥₹]\\s*\\d+(?:,\\d{3})*(?:\\.\\d+)?)|(\\d+(?:,\\d{3})*(?:\\.\\d+)?\\s*(?:usd|eur|gbp|inr|dollars?|cents?))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PERCENT_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?%",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b\\d+(?:,\\d{3})*(?:\\.\\d+)?\\b"
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z0-9_]+");

    /**
     * Preprocesses input text into a list of normalized tokens.
     *
     * @param rawText the raw email text (may include subject and body)
     * @return an unmodifiable list of cleaned and normalized tokens
     */
    public List<String> tokenize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        // 1. Lowercase normalization
        String text = rawText.toLowerCase(Locale.ROOT);

        // 2. Normalize URLs
        text = URL_PATTERN.matcher(text).replaceAll(" __url__ ");

        // 3. Normalize Email Addresses
        text = EMAIL_PATTERN.matcher(text).replaceAll(" __email__ ");

        // 4. Normalize Currency and Monetary amounts
        text = CURRENCY_PATTERN.matcher(text).replaceAll(" __money__ ");

        // 5. Normalize Percentages (e.g. 500% profit, 80% discount)
        text = PERCENT_PATTERN.matcher(text).replaceAll(" __percent__ ");

        // 6. Normalize standalone numbers (e.g., telephone, amounts)
        text = NUMBER_PATTERN.matcher(text).replaceAll(" __number__ ");

        // 7. Token extraction
        List<String> tokens = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String token = matcher.group();
            // Filter out single character non-special tokens unless meaningful
            if (token.length() > 1 || token.startsWith("__")) {
                tokens.add(token);
            }
        }

        return Collections.unmodifiableList(tokens);
    }

    /**
     * Combines subject and body into a unified text string.
     * Subject words are prefixed to provide early signal weighting.
     *
     * @param subject optional email subject
     * @param body email body content
     * @return combined text representation
     */
    public String combineSubjectAndBody(String subject, String body) {
        StringBuilder sb = new StringBuilder();
        if (subject != null && !subject.isBlank()) {
            sb.append(subject.trim()).append("\n\n");
        }
        if (body != null && !body.isBlank()) {
            sb.append(body.trim());
        }
        return sb.toString();
    }
}
