package com.mailshield.ml;

import com.mailshield.model.PredictionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NaiveBayesClassifierTest {

    private NaiveBayesClassifier classifier;
    private TextPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new TextPreprocessor();
        classifier = new NaiveBayesClassifier(preprocessor);
    }

    private void trainSimpleModel() {
        List<TrainingRecord> records = List.of(
                new TrainingRecord("SPAM", "urgent lottery winner claim cash prize dollar free bonus"),
                new TrainingRecord("SPAM", "congratulations account suspended verify password wire money immediate"),
                new TrainingRecord("SPAM", "unclaimed inheritance crypto bitcoin profit million deposit now"),
                new TrainingRecord("HAM", "team sprint meeting agenda schedule review project status"),
                new TrainingRecord("HAM", "attached quarterly financial report presentation slides draft"),
                new TrainingRecord("HAM", "lunch with engineering team cafeteria conference call discussion")
        );
        classifier.train(records);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when classifying before training")
    void testUntrainedModelThrows() {
        assertFalse(classifier.isTrained());
        assertThrows(IllegalStateException.class, () -> classifier.classify("subject", "body"));
    }

    @Test
    @DisplayName("Should reject empty or null training data")
    void testEmptyTrainingDataThrows() {
        assertThrows(IllegalArgumentException.class, () -> classifier.train(null));
        assertThrows(IllegalArgumentException.class, () -> classifier.train(Collections.emptyList()));
    }

    @Test
    @DisplayName("Should reject training dataset missing either SPAM or HAM class")
    void testSingleClassTrainingDataThrows() {
        List<TrainingRecord> onlySpam = List.of(
                new TrainingRecord("SPAM", "buy cheap crypto prize"),
                new TrainingRecord("SPAM", "lottery winner account suspended")
        );
        assertThrows(IllegalArgumentException.class, () -> classifier.train(onlySpam));
    }

    @Test
    @DisplayName("Should train model and calculate accurate priors and vocabulary size")
    void testModelTrainingStats() {
        trainSimpleModel();
        assertTrue(classifier.isTrained());
        assertEquals(6, classifier.getTotalDocCount());
        assertEquals(3, classifier.getSpamDocCount());
        assertEquals(3, classifier.getHamDocCount());
        assertEquals(0.5, classifier.getPriorSpam(), 0.0001);
        assertEquals(0.5, classifier.getPriorHam(), 0.0001);
        assertTrue(classifier.getVocabularySize() > 10);
    }

    @Test
    @DisplayName("Should accurately classify distinct SPAM message")
    void testSpamClassification() {
        trainSimpleModel();
        PredictionResult result = classifier.classify("Claim your lottery prize", "Congratulations winner! Free cash bonus deposit now.");
        assertEquals("SPAM", result.predictedLabel());
        assertTrue(result.spamProbability() > 0.70);
        assertTrue(result.hamProbability() < 0.30);
        assertFalse(result.topSpamIndicators().isEmpty());
    }

    @Test
    @DisplayName("Should accurately classify distinct HAM message")
    void testHamClassification() {
        trainSimpleModel();
        PredictionResult result = classifier.classify("Sprint review meeting", "Hi team, please find the quarterly presentation agenda attached.");
        assertEquals("HAM", result.predictedLabel());
        assertTrue(result.hamProbability() > 0.70);
        assertTrue(result.spamProbability() < 0.30);
        assertFalse(result.topHamIndicators().isEmpty());
    }

    @Test
    @DisplayName("Probabilities should always normalize to 1.0 (Log-Sum-Exp test)")
    void testProbabilityNormalization() {
        trainSimpleModel();
        PredictionResult result = classifier.classify("Subject test", "Crypto bitcoin wire urgent verify presentation");
        double sum = result.spamProbability() + result.hamProbability();
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    @DisplayName("Laplace smoothing should prevent zero probability for unseen class words")
    void testLaplaceSmoothing() {
        trainSimpleModel();
        // "agenda" appeared ONLY in HAM, but classifying a message with "agenda" under SPAM should not crash or produce -Infinity
        PredictionResult result = classifier.classify("agenda update", "agenda discussion");
        assertFalse(Double.isInfinite(result.totalLogScoreSpam()));
        assertFalse(Double.isNaN(result.totalLogScoreSpam()));
        assertFalse(Double.isInfinite(result.totalLogScoreHam()));
        assertFalse(Double.isNaN(result.totalLogScoreHam()));
    }

    @Test
    @DisplayName("Should handle unknown out-of-vocabulary words without skewing result")
    void testUnknownWords() {
        trainSimpleModel();
        PredictionResult result = classifier.classify("Xylophone Zephyr", "Quirky Quantum Quintessence");
        // All words are unknown, should fall back cleanly
        assertTrue(result.unknownTokens() >= 3);
        double sum = result.spamProbability() + result.hamProbability();
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    @DisplayName("Should safely handle empty and whitespace-only email text")
    void testEmptyEmail() {
        trainSimpleModel();
        PredictionResult result = classifier.classify("", "   ");
        assertEquals(0, result.totalTokens());
        assertEquals(0.5, result.spamProbability(), 0.001);
        assertEquals(0.5, result.hamProbability(), 0.001);
        assertNotNull(result.explanation());
    }

    @Test
    @DisplayName("Repeated words in Multinomial model should scale likelihood influence")
    void testRepeatedWordsInfluence() {
        trainSimpleModel();
        PredictionResult singleWinner = classifier.classify("", "winner");
        PredictionResult tripleWinner = classifier.classify("", "winner winner winner");

        assertTrue(tripleWinner.spamProbability() > singleWinner.spamProbability());
    }
}
