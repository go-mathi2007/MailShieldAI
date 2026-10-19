package com.mailshield.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelEvaluatorTest {

    private ModelEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ModelEvaluator(new TextPreprocessor(), 0.25, 42L);
    }

    @Test
    @DisplayName("Should evaluate stratified test set with accurate metrics and confusion matrix")
    void testEvaluationOnSyntheticDataset() {
        List<TrainingRecord> records = new ArrayList<>();
        // Add 10 spam records
        for (int i = 0; i < 10; i++) {
            records.add(new TrainingRecord("SPAM", "urgent prize lottery winner claim money " + i));
        }
        // Add 10 ham records
        for (int i = 0; i < 10; i++) {
            records.add(new TrainingRecord("HAM", "team sprint meeting project agenda report " + i));
        }

        EvaluationResult result = evaluator.evaluate(records, 0.20, 42L);

        assertNotNull(result);
        assertEquals(20, result.totalDatasetSize());
        assertEquals(16, result.trainSetSize());
        assertEquals(4, result.testSetSize());
        assertEquals(2, result.testSpamCount());
        assertEquals(2, result.testHamCount());

        ConfusionMatrix cm = result.confusionMatrix();
        assertNotNull(cm);
        assertEquals(4, cm.total());

        assertTrue(result.accuracy() >= 0.0 && result.accuracy() <= 1.0);
        assertTrue(result.precision() >= 0.0 && result.precision() <= 1.0);
        assertTrue(result.recall() >= 0.0 && result.recall() <= 1.0);
        assertTrue(result.f1Score() >= 0.0 && result.f1Score() <= 1.0);
        assertTrue(result.isSmallDataset()); // 20 < 50
    }

    @Test
    @DisplayName("Should throw exception if dataset is too small to evaluate")
    void testTooSmallDatasetThrows() {
        List<TrainingRecord> tiny = List.of(
                new TrainingRecord("SPAM", "spam prize"),
                new TrainingRecord("HAM", "ham meeting")
        );
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(tiny));
    }
}
