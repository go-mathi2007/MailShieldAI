package com.mailshield.service;

import com.mailshield.ml.EvaluationResult;
import com.mailshield.ml.ModelEvaluator;
import com.mailshield.ml.NaiveBayesClassifier;
import com.mailshield.ml.TrainingRecord;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for loading, parsing, and training the Naive Bayes model
 * from the emails CSV dataset.
 */
@Service
public class DatasetTrainingService {

    private static final Logger log = LoggerFactory.getLogger(DatasetTrainingService.class);

    private final NaiveBayesClassifier classifier;
    private final ModelEvaluator modelEvaluator;
    private final ResourceLoader resourceLoader;
    private final String datasetPath;

    private final List<TrainingRecord> currentRecords = new ArrayList<>();
    private EvaluationResult cachedEvaluation = null;

    public DatasetTrainingService(
            NaiveBayesClassifier classifier,
            ModelEvaluator modelEvaluator,
            ResourceLoader resourceLoader,
            @Value("${mailshield.dataset.path:classpath:dataset/emails.csv}") String datasetPath
    ) {
        this.classifier = classifier;
        this.modelEvaluator = modelEvaluator;
        this.resourceLoader = resourceLoader;
        this.datasetPath = datasetPath;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing MailShield AI Classifier from dataset: {}", datasetPath);
        loadAndTrainDefaultDataset();
    }

    public synchronized void loadAndTrainDefaultDataset() {
        try {
            Resource resource = resourceLoader.getResource(datasetPath);
            if (!resource.exists()) {
                log.warn("Default dataset resource not found at: {}. Model remains untrained until data is provided.", datasetPath);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                loadAndTrainFromStream(is, datasetPath);
            }
        } catch (Exception e) {
            log.error("Failed to load default dataset from: {}. Error: {}", datasetPath, e.getMessage(), e);
        }
    }

    /**
     * Loads records from an input stream, parses RFC-4180 CSV, trains the model,
     * and evaluates model performance.
     *
     * @param inputStream source input stream
     * @param sourceName name of source for logging
     * @return number of valid records loaded
     * @throws IOException on I/O error
     * @throws CsvException on CSV parsing error
     */
    public synchronized int loadAndTrainFromStream(InputStream inputStream, String sourceName)
            throws IOException, CsvException {
        List<TrainingRecord> records = parseCsv(inputStream);

        if (records.isEmpty()) {
            throw new IllegalArgumentException("No valid records found in dataset source: " + sourceName);
        }

        classifier.train(records);

        currentRecords.clear();
        currentRecords.addAll(records);

        // Run stratified evaluation on the loaded records
        try {
            cachedEvaluation = modelEvaluator.evaluate(currentRecords);
        } catch (Exception e) {
            log.warn("Model evaluation skipped: {}", e.getMessage());
            cachedEvaluation = null;
        }

        log.info("Successfully trained model from {} with {} records (SPAM: {}, HAM: {})",
                sourceName, records.size(), classifier.getSpamDocCount(), classifier.getHamDocCount());

        return records.size();
    }

    /**
     * Parses CSV records using OpenCSV with quote and escape handling.
     */
    public List<TrainingRecord> parseCsv(InputStream inputStream) throws IOException, CsvException {
        List<TrainingRecord> records = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            List<String[]> allRows = csvReader.readAll();
            if (allRows.isEmpty()) {
                return Collections.emptyList();
            }

            // Find label and text header column indexes
            String[] header = allRows.get(0);
            int labelIdx = -1;
            int textIdx = -1;

            for (int i = 0; i < header.length; i++) {
                String col = header[i].trim().toLowerCase();
                if (col.equals("label") || col.equals("target") || col.equals("category") || col.equals("class")) {
                    labelIdx = i;
                } else if (col.equals("text") || col.equals("email") || col.equals("message") || col.equals("body") || col.equals("content")) {
                    textIdx = i;
                }
            }

            // Fallback to default columns if standard header not found
            if (labelIdx == -1 || textIdx == -1) {
                if (header.length >= 2) {
                    labelIdx = 0;
                    textIdx = 1;
                } else {
                    throw new IllegalArgumentException("CSV must have at least 2 columns (label, text). Found: " + header.length);
                }
            }

            for (int rowIdx = 1; rowIdx < allRows.size(); rowIdx++) {
                String[] row = allRows.get(rowIdx);
                if (row.length <= Math.max(labelIdx, textIdx)) {
                    continue;
                }

                String label = row[labelIdx] != null ? row[labelIdx].trim() : "";
                String text = row[textIdx] != null ? row[textIdx].trim() : "";

                if (label.isEmpty() || text.isEmpty()) {
                    continue;
                }

                String normalizedLabel;
                if (label.equalsIgnoreCase("spam") || label.equals("1")) {
                    normalizedLabel = TrainingRecord.LABEL_SPAM;
                } else if (label.equalsIgnoreCase("ham") || label.equalsIgnoreCase("legit") || label.equals("0")) {
                    normalizedLabel = TrainingRecord.LABEL_HAM;
                } else {
                    continue; // Skip unrecognized label
                }

                records.add(new TrainingRecord(normalizedLabel, text));
            }
        }

        return records;
    }

    public synchronized List<TrainingRecord> getCurrentRecords() {
        return Collections.unmodifiableList(currentRecords);
    }

    public synchronized EvaluationResult getEvaluationResult() {
        if (cachedEvaluation == null && !currentRecords.isEmpty()) {
            try {
                cachedEvaluation = modelEvaluator.evaluate(currentRecords);
            } catch (Exception e) {
                log.warn("Evaluation failed: {}", e.getMessage());
            }
        }
        return cachedEvaluation;
    }
}
