package com.mailshield.service;

import com.mailshield.ml.ModelEvaluator;
import com.mailshield.ml.NaiveBayesClassifier;
import com.mailshield.ml.TextPreprocessor;
import com.mailshield.ml.TrainingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatasetTrainingServiceTest {

    private DatasetTrainingService service;
    private NaiveBayesClassifier classifier;

    @BeforeEach
    void setUp() {
        TextPreprocessor preprocessor = new TextPreprocessor();
        classifier = new NaiveBayesClassifier(preprocessor);
        ModelEvaluator evaluator = new ModelEvaluator(preprocessor, 0.20, 42L);
        service = new DatasetTrainingService(classifier, evaluator, new DefaultResourceLoader(), "classpath:dataset/emails.csv");
    }

    @Test
    @DisplayName("Should parse CSV with quotes, commas, and multiline text")
    void testParseComplexCsv() throws Exception {
        String csvData = """
                label,text
                spam,"Congratulations! You won $1,000,000. Click here, claim now!"
                ham,"Hi John, please find the quarterly report, budget spreadsheet, and agenda attached."
                spam,"URGENT: Your account, billing status, and card require verification."
                ham,"Team meeting scheduled for Monday at 10:00 AM, in room 4B."
                """;

        List<TrainingRecord> records = service.parseCsv(
                new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(4, records.size());
        assertEquals("SPAM", records.get(0).label());
        assertTrue(records.get(0).text().contains("$1,000,000"));
        assertEquals("HAM", records.get(1).label());
        assertTrue(records.get(1).text().contains("quarterly report"));
    }

    @Test
    @DisplayName("Should train classifier and produce valid evaluation when stream loaded")
    void testLoadAndTrainFromStream() throws Exception {
        String csvData = """
                label,text
                spam,"Free crypto bitcoin reward guaranteed profit"
                spam,"Urgent wire transfer lottery winner prize money"
                spam,"Verify account password immediately suspended"
                ham,"Project retrospective meeting agenda review notes"
                ham,"Code review feedback submitted on github pull request"
                ham,"Quarterly earnings presentation slide deck attached"
                """;

        int count = service.loadAndTrainFromStream(
                new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)),
                "test-stream"
        );

        assertEquals(6, count);
        assertTrue(classifier.isTrained());
        assertEquals(3, classifier.getSpamDocCount());
        assertEquals(3, classifier.getHamDocCount());
        assertNotNull(service.getEvaluationResult());
    }
}
