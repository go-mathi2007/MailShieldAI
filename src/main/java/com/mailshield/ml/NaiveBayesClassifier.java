package com.mailshield.ml;

import com.mailshield.dto.IndicatorToken;
import com.mailshield.model.PredictionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Multinomial Naive Bayes Classifier implemented in pure Java.
 *
 * <p>Mathematical Overview:</p>
 * <ul>
 *   <li><b>Priors:</b> \(P(c) = \frac{N_c}{N_{\text{total}}}\), where \(N_c\) is the number of training emails in class \(c\).</li>
 *   <li><b>Likelihood with Laplace Smoothing:</b>
 *     \[P(w|c) = \frac{\text{count}(w, c) + \alpha}{\sum_{w' \in V} \text{count}(w', c) + \alpha |V|}\]
 *     where \(|V|\) is the size of the combined vocabulary and \(\alpha > 0\) is the smoothing parameter (default 1.0).
 *   </li>
 *   <li><b>Multinomial Document Score:</b>
 *     \[\log P(c|D) \propto \log P(c) + \sum_{w \in D} \text{count}(w, D) \cdot \log P(w|c)\]
 *     Repeated words in the document naturally increase their influence proportional to their term frequency.
 *   </li>
 *   <li><b>Posterior Normalization (Log-Sum-Exp):</b>
 *     To prevent numerical overflow or underflow with exponentiation, posterior probabilities
 *     are computed via the stable log-sum-exp transformation:
 *     \[P(c_1|D) = \frac{e^{\log P(c_1|D) - M}}{e^{\log P(c_1|D) - M} + e^{\log P(c_2|D) - M}}\]
 *     where \(M = \max(\log P(c_1|D), \log P(c_2|D))\).
 *   </li>
 *   <li><b>Out-of-Vocabulary (OOV) Handling:</b>
 *     Tokens not observed in any class during training are ignored during inference, ensuring
 *     extraneous novel words do not distort the evidence balance.
 *   </li>
 * </ul>
 */
@Component
public class NaiveBayesClassifier {

    private static final Logger log = LoggerFactory.getLogger(NaiveBayesClassifier.class);

    private final TextPreprocessor preprocessor;
    private double alpha = 1.0;

    // Model state
    private boolean isTrained = false;
    private int spamDocCount = 0;
    private int hamDocCount = 0;
    private int totalDocCount = 0;

    private long spamTotalWordCount = 0;
    private long hamTotalWordCount = 0;

    private final Map<String, Integer> spamWordCounts = new HashMap<>();
    private final Map<String, Integer> hamWordCounts = new HashMap<>();
    private final Set<String> vocabulary = new HashSet<>();

    // Precomputed log values for performance
    private double logPriorSpam = 0.0;
    private double logPriorHam = 0.0;
    private double spamDenominator = 0.0;
    private double hamDenominator = 0.0;

    public NaiveBayesClassifier(TextPreprocessor preprocessor) {
        this.preprocessor = Objects.requireNonNull(preprocessor, "preprocessor must not be null");
    }

    /**
     * Sets the Laplace smoothing parameter alpha.
     *
     * @param alpha smoothing parameter, must be positive
     */
    public synchronized void setAlpha(double alpha) {
        if (alpha <= 0.0) {
            throw new IllegalArgumentException("Laplace smoothing alpha must be strictly positive, given: " + alpha);
        }
        this.alpha = alpha;
        if (isTrained) {
            recalculateDenominators();
        }
    }

    public double getAlpha() {
        return alpha;
    }

    public synchronized boolean isTrained() {
        return isTrained;
    }

    public synchronized int getVocabularySize() {
        return vocabulary.size();
    }

    public synchronized int getSpamDocCount() {
        return spamDocCount;
    }

    public synchronized int getHamDocCount() {
        return hamDocCount;
    }

    public synchronized int getTotalDocCount() {
        return totalDocCount;
    }

    public synchronized double getPriorSpam() {
        return totalDocCount == 0 ? 0.0 : (double) spamDocCount / totalDocCount;
    }

    public synchronized double getPriorHam() {
        return totalDocCount == 0 ? 0.0 : (double) hamDocCount / totalDocCount;
    }

    /**
     * Trains the Naive Bayes model on a collection of labelled records.
     *
     * @param records labelled records
     * @throws IllegalArgumentException if the record list is empty, null, or missing a class
     */
    public synchronized void train(Collection<TrainingRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Cannot train model: training dataset is null or empty.");
        }

        // Reset previous training state
        resetState();

        for (TrainingRecord record : records) {
            List<String> tokens = preprocessor.tokenize(record.text());
            if (record.isSpam()) {
                spamDocCount++;
                for (String token : tokens) {
                    spamWordCounts.merge(token, 1, Integer::sum);
                    spamTotalWordCount++;
                    vocabulary.add(token);
                }
            } else if (record.isHam()) {
                hamDocCount++;
                for (String token : tokens) {
                    hamWordCounts.merge(token, 1, Integer::sum);
                    hamTotalWordCount++;
                    vocabulary.add(token);
                }
            }
        }

        totalDocCount = spamDocCount + hamDocCount;

        if (spamDocCount == 0 || hamDocCount == 0) {
            throw new IllegalArgumentException(String.format(
                    "Training dataset must contain both SPAM and HAM records. Found SPAM: %d, HAM: %d",
                    spamDocCount, hamDocCount
            ));
        }

        // Compute log priors
        logPriorSpam = Math.log((double) spamDocCount / totalDocCount);
        logPriorHam = Math.log((double) hamDocCount / totalDocCount);

        recalculateDenominators();
        isTrained = true;

        log.info("Model trained successfully: {} total docs ({} SPAM, {} HAM), vocabulary size: {}, smoothing alpha: {}",
                totalDocCount, spamDocCount, hamDocCount, vocabulary.size(), alpha);
    }

    private void recalculateDenominators() {
        int vocabSize = vocabulary.size();
        spamDenominator = Math.log((double) spamTotalWordCount + (alpha * vocabSize));
        hamDenominator = Math.log((double) hamTotalWordCount + (alpha * vocabSize));
    }

    private void resetState() {
        isTrained = false;
        spamDocCount = 0;
        hamDocCount = 0;
        totalDocCount = 0;
        spamTotalWordCount = 0;
        hamTotalWordCount = 0;
        spamWordCounts.clear();
        hamWordCounts.clear();
        vocabulary.clear();
    }

    /**
     * Classifies an email and returns detailed inference statistics.
     *
     * @param subject email subject line (optional)
     * @param body email body text
     * @return full prediction result with normalized probabilities and token indicators
     * @throws IllegalStateException if the model has not been trained
     */
    public PredictionResult classify(String subject, String body) {
        if (!isTrained) {
            throw new IllegalStateException("Naive Bayes Classifier has not been trained yet. Please load training data first.");
        }

        String combinedText = preprocessor.combineSubjectAndBody(subject, body);
        List<String> tokens = preprocessor.tokenize(combinedText);

        return classifyTokens(tokens);
    }

    /**
     * Classifies preprocessed tokens.
     *
     * @param tokens list of normalized tokens
     * @return PredictionResult
     */
    public synchronized PredictionResult classifyTokens(List<String> tokens) {
        if (!isTrained) {
            throw new IllegalStateException("Naive Bayes Classifier has not been trained yet.");
        }

        if (tokens == null || tokens.isEmpty()) {
            // Safe fallback to class priors when no words are provided
            double spamProb = getPriorSpam();
            double hamProb = getPriorHam();
            String label = spamProb >= hamProb ? TrainingRecord.LABEL_SPAM : TrainingRecord.LABEL_HAM;
            return new PredictionResult(
                    label,
                    round(spamProb, 4),
                    round(hamProb, 4),
                    logPriorSpam,
                    logPriorHam,
                    0.0,
                    0.0,
                    logPriorSpam,
                    logPriorHam,
                    0,
                    0,
                    0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "Message contained no extractable words. Classification defaulted to training dataset class prior probabilities.",
                    PredictionResult.DISCLAIMER_TEXT
            );
        }

        // Count term frequency in the query document
        Map<String, Integer> termFrequencies = new LinkedHashMap<>();
        int unknownTokens = 0;
        int recognizedTokens = 0;

        for (String token : tokens) {
            if (vocabulary.contains(token)) {
                termFrequencies.merge(token, 1, Integer::sum);
                recognizedTokens++;
            } else {
                unknownTokens++;
            }
        }

        double logLikelihoodSpam = 0.0;
        double logLikelihoodHam = 0.0;

        List<IndicatorToken> indicators = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String token = entry.getKey();
            int count = entry.getValue();

            int spamCount = spamWordCounts.getOrDefault(token, 0);
            int hamCount = hamWordCounts.getOrDefault(token, 0);

            // Laplace smoothed log likelihoods: log(count + alpha) - log(totalWords + alpha * |V|)
            double tokenLogLikelihoodSpam = Math.log(spamCount + alpha) - spamDenominator;
            double tokenLogLikelihoodHam = Math.log(hamCount + alpha) - hamDenominator;

            // Multinomial Naive Bayes incorporates repeated terms via multiplication with term count
            logLikelihoodSpam += count * tokenLogLikelihoodSpam;
            logLikelihoodHam += count * tokenLogLikelihoodHam;

            // Log-likelihood ratio for explainability: positive = spam indicator, negative = ham indicator
            double llr = tokenLogLikelihoodSpam - tokenLogLikelihoodHam;
            double spamLikelihoodLinear = Math.exp(tokenLogLikelihoodSpam);
            double hamLikelihoodLinear = Math.exp(tokenLogLikelihoodHam);

            String category = llr > 0.05 ? "SPAM_INDICATOR" : (llr < -0.05 ? "HAM_INDICATOR" : "NEUTRAL");
            indicators.add(new IndicatorToken(
                    token,
                    count,
                    round(spamLikelihoodLinear, 6),
                    round(hamLikelihoodLinear, 6),
                    round(llr, 4),
                    category
            ));
        }

        double totalLogScoreSpam = logPriorSpam + logLikelihoodSpam;
        double totalLogScoreHam = logPriorHam + logLikelihoodHam;

        // Log-Sum-Exp normalization to compute accurate posterior probabilities without underflow
        double maxScore = Math.max(totalLogScoreSpam, totalLogScoreHam);
        double expSpam = Math.exp(totalLogScoreSpam - maxScore);
        double expHam = Math.exp(totalLogScoreHam - maxScore);
        double posteriorSum = expSpam + expHam;

        double spamProbability = expSpam / posteriorSum;
        double hamProbability = expHam / posteriorSum;

        String predictedLabel = spamProbability >= hamProbability ? TrainingRecord.LABEL_SPAM : TrainingRecord.LABEL_HAM;

        // Separate and rank top indicators by net evidence contribution: count * LLR
        List<IndicatorToken> topSpamIndicators = indicators.stream()
                .filter(i -> i.logLikelihoodRatio() > 0)
                .sorted((a, b) -> Double.compare(
                        b.logLikelihoodRatio() * b.countInDoc(),
                        a.logLikelihoodRatio() * a.countInDoc()
                ))
                .limit(8)
                .toList();

        List<IndicatorToken> topHamIndicators = indicators.stream()
                .filter(i -> i.logLikelihoodRatio() < 0)
                .sorted((a, b) -> Double.compare(
                        a.logLikelihoodRatio() * a.countInDoc(),
                        b.logLikelihoodRatio() * b.countInDoc()
                ))
                .limit(8)
                .toList();

        String explanation = generateExplanation(predictedLabel, spamProbability, topSpamIndicators, topHamIndicators, recognizedTokens, unknownTokens);

        return new PredictionResult(
                predictedLabel,
                round(spamProbability, 4),
                round(hamProbability, 4),
                round(logPriorSpam, 4),
                round(logPriorHam, 4),
                round(logLikelihoodSpam, 4),
                round(logLikelihoodHam, 4),
                round(totalLogScoreSpam, 4),
                round(totalLogScoreHam, 4),
                tokens.size(),
                recognizedTokens,
                unknownTokens,
                topSpamIndicators,
                topHamIndicators,
                explanation,
                PredictionResult.DISCLAIMER_TEXT
        );
    }

    private String generateExplanation(String label, double spamProb,
                                       List<IndicatorToken> spamInds,
                                       List<IndicatorToken> hamInds,
                                       int recognized, int unknown) {
        StringBuilder sb = new StringBuilder();
        if (TrainingRecord.LABEL_SPAM.equals(label)) {
            sb.append(String.format("Classified as SPAM with %.1f%% confidence. ", spamProb * 100.0));
            if (!spamInds.isEmpty()) {
                sb.append("Key suspicious tokens: ");
                for (int i = 0; i < Math.min(4, spamInds.size()); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("'").append(spamInds.get(i).token()).append("'");
                }
                sb.append(". ");
            }
        } else {
            sb.append(String.format("Classified as HAM (Legitimate) with %.1f%% confidence. ", (1.0 - spamProb) * 100.0));
            if (!hamInds.isEmpty()) {
                sb.append("Key legitimate tokens: ");
                for (int i = 0; i < Math.min(4, hamInds.size()); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("'").append(hamInds.get(i).token()).append("'");
                }
                sb.append(". ");
            }
        }

        if (unknown > 0) {
            sb.append(String.format("(Evaluated %d recognized words; %d out-of-vocabulary words were ignored).", recognized, unknown));
        } else {
            sb.append(String.format("(Evaluated %d recognized words).", recognized));
        }

        return sb.toString();
    }

    private static double round(double val, int decimals) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return val;
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
