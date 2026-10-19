package com.mailshield.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextPreprocessorTest {

    private TextPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new TextPreprocessor();
    }

    @Test
    @DisplayName("Should lowercase and extract standard words")
    void testStandardWords() {
        List<String> tokens = preprocessor.tokenize("Hello World! Meeting Tomorrow at Office.");
        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
        assertTrue(tokens.contains("meeting"));
        assertTrue(tokens.contains("tomorrow"));
        assertTrue(tokens.contains("office"));
    }

    @Test
    @DisplayName("Should abstract URLs into special token")
    void testUrlAbstraction() {
        List<String> tokens = preprocessor.tokenize("Visit https://suspicious-site.xyz/login.php for free prize");
        assertTrue(tokens.contains("__url__"));
        assertTrue(tokens.contains("free"));
        assertTrue(tokens.contains("prize"));
    }

    @Test
    @DisplayName("Should abstract currency amounts into special token")
    void testCurrencyAbstraction() {
        List<String> tokens = preprocessor.tokenize("Claim your $1,000,000 lottery payout today!");
        assertTrue(tokens.contains("__money__"));
        assertTrue(tokens.contains("claim"));
        assertTrue(tokens.contains("lottery"));
    }

    @Test
    @DisplayName("Should abstract percentages into special token")
    void testPercentAbstraction() {
        List<String> tokens = preprocessor.tokenize("Guaranteed 500% profit in 24 hours");
        assertTrue(tokens.contains("__percent__"));
        assertTrue(tokens.contains("guaranteed"));
        assertTrue(tokens.contains("profit"));
    }

    @Test
    @DisplayName("Should abstract email addresses into special token")
    void testEmailAbstraction() {
        List<String> tokens = preprocessor.tokenize("Send bank details to support@bank-verify.com immediately");
        assertTrue(tokens.contains("__email__"));
        assertTrue(tokens.contains("bank"));
    }

    @Test
    @DisplayName("Should handle null and empty input safely")
    void testEmptyAndNull() {
        assertTrue(preprocessor.tokenize(null).isEmpty());
        assertTrue(preprocessor.tokenize("   ").isEmpty());
        assertTrue(preprocessor.tokenize("").isEmpty());
    }

    @Test
    @DisplayName("Should combine subject and body")
    void testCombineSubjectAndBody() {
        String combined = preprocessor.combineSubjectAndBody("Urgent Notice", "Please check your account");
        assertTrue(combined.contains("Urgent Notice"));
        assertTrue(combined.contains("Please check your account"));
    }
}
