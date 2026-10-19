package com.mailshield.ml;

import com.mailshield.model.PredictionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Model Evaluator for MailShield AI.
 * Performs reproducible stratified train-test splits, prevents data leakage,
 * evaluates classifier performance, and computes standard ML metrics.
 */
@Component
public class ModelEvaluator {

    private final TextPreprocessor preprocessor;
    private final double defaultTestRatio;
    private final long defaultRandomSeed;

    public ModelEvaluator(
            TextPreprocessor preprocessor,
            @Value("${mailshield.evaluation.test-ratio:0.20}") double defaultTestRatio,
            @Value("${mailshield.evaluation.random-seed:42}") long defaultRandomSeed
    ) {
        this.preprocessor = preprocessor;
        this.defaultTestRatio = defaultTestRatio;
        this.defaultRandomSeed = defaultRandomSeed;
    }

    /**
     * Evaluates the dataset using stratified train/test split.
     *
     * @param allRecords all labelled records
     * @return EvaluationResult
     */
    public EvaluationResult evaluate(List<TrainingRecord> allRecords) {
        return evaluate(allRecords, defaultTestRatio, defaultRandomSeed);
    }

    /**
     * Evaluates the dataset using specified test ratio and random seed.
     *
     * @param allRecords all labelled records
     * @param testRatio proportion for test split (0.05 to 0.50)
     * @param seed random seed for reproducibility
     * @return EvaluationResult
     */
    public EvaluationResult evaluate(List<TrainingRecord> allRecords, double testRatio, long seed) {
        if (allRecords == null || allRecords.size() < 4) {
            throw new IllegalArgumentException("Dataset too small for evaluation. At least 4 records required.");
        }

        // Separate by class for stratified split
        List<TrainingRecord> spams = new ArrayList<>();
        List<TrainingRecord> hams = new ArrayList<>();

        for (TrainingRecord r : allRecords) {
            if (r.isSpam()) {
                spams.add(r);
            } else {
                hams.add(r);
            }
        }

        if (spams.size() < 2 || hams.size() < 2) {
            throw new IllegalArgumentException("Stratified evaluation requires at least 2 SPAM and 2 HAM records.");
        }

        // Shuffle with reproducible seed
        Random rand = new Random(seed);
        Collections.shuffle(spams, rand);
        Collections.shuffle(hams, rand);

        int spamTestCount = Math.max(1, (int) Math.round(spams.size() * testRatio));
        int hamTestCount = Math.max(1, (int) Math.round(hams.size() * testRatio));

        // Ensure we don't take all records into test
        if (spamTestCount >= spams.size()) spamTestCount = spams.size() - 1;
        if (hamTestCount >= hams.size()) hamTestCount = hams.size() - 1;

        List<TrainingRecord> testRecords = new ArrayList<>();
        List<TrainingRecord> trainRecords = new ArrayList<>();

        testRecords.addAll(spams.subList(0, spamTestCount));
        trainRecords.addAll(spams.subList(spamTestCount, spams.size()));

        testRecords.addAll(hams.subList(0, hamTestCount));
        trainRecords.addAll(hams.subList(hamTestCount, hams.size()));

        // Train a clean evaluation model exclusively on the training split to prevent data leakage
        NaiveBayesClassifier evalClassifier = new NaiveBayesClassifier(preprocessor);
        evalClassifier.train(trainRecords);

        int tp = 0; // Actual SPAM, Predicted SPAM
        int fp = 0; // Actual HAM,  Predicted SPAM
        int tn = 0; // Actual HAM,  Predicted HAM
        int fn = 0; // Actual SPAM, Predicted HAM

        for (TrainingRecord testRec : testRecords) {
            PredictionResult pred = evalClassifier.classify("", testRec.text());
            boolean predictedSpam = TrainingRecord.LABEL_SPAM.equalsIgnoreCase(pred.predictedLabel());

            if (testRec.isSpam()) {
                if (predictedSpam) {
                    tp++;
                } else {
                    fn++;
                }
            } else {
                if (predictedSpam) {
                    fp++;
                } else {
                    tn++;
                }
            }
        }

        int totalTest = testRecords.size();
        double accuracy = totalTest == 0 ? 0.0 : (double) (tp + tn) / totalTest;
        double precision = (tp + fp) == 0 ? 0.0 : (double) tp / (tp + fp);
        double recall = (tp + fn) == 0 ? 0.0 : (double) tp / (tp + fn);
        double f1 = (precision + recall) == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);

        boolean isSmallDataset = allRecords.size() < 50;
        String note;
        if (isSmallDataset) {
            note = "Notice: The current dataset contains fewer than 50 records. Metrics are computed on a small sample set and serve as a functional demonstration rather than enterprise benchmark numbers.";
        } else {
            note = String.format("Evaluated on %d test emails (stratified %.0f%% holdout) with zero data leakage. Metrics reflect dataset quality and vocabulary coverage.",
                    totalTest, testRatio * 100.0);
        }

        ConfusionMatrix cm = new ConfusionMatrix(tp, fp, tn, fn);

        int trainSpam = (int) trainRecords.stream().filter(TrainingRecord::isSpam).count();
        int trainHam = trainRecords.size() - trainSpam;

        return new EvaluationResult(
                allRecords.size(),
                trainRecords.size(),
                testRecords.size(),
                trainSpam,
                trainHam,
                spamTestCount,
                hamTestCount,
                round(accuracy, 4),
                round(precision, 4),
                round(recall, 4),
                round(f1, 4),
                cm,
                isSmallDataset,
                note
        );
    }

    private static double round(double val, int decimals) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return val;
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
