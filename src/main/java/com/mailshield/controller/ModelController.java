package com.mailshield.controller;

import com.mailshield.dto.ModelInfoResponse;
import com.mailshield.ml.EvaluationResult;
import com.mailshield.ml.NaiveBayesClassifier;
import com.mailshield.service.DatasetTrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ModelController {

    private final NaiveBayesClassifier classifier;
    private final DatasetTrainingService datasetTrainingService;

    public ModelController(NaiveBayesClassifier classifier, DatasetTrainingService datasetTrainingService) {
        this.classifier = classifier;
        this.datasetTrainingService = datasetTrainingService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "appName", "MailShield AI",
                "version", "1.0.0",
                "modelTrained", classifier.isTrained(),
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/model/info")
    public ResponseEntity<ModelInfoResponse> getModelInfo() {
        EvaluationResult eval = datasetTrainingService.getEvaluationResult();
        ModelInfoResponse response = new ModelInfoResponse(
                classifier.isTrained(),
                "Multinomial Naive Bayes with Laplace Smoothing (alpha=" + classifier.getAlpha() + ")",
                classifier.getAlpha(),
                classifier.getTotalDocCount(),
                classifier.getSpamDocCount(),
                classifier.getHamDocCount(),
                classifier.getVocabularySize(),
                classifier.getPriorSpam(),
                classifier.getPriorHam(),
                eval
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evaluation")
    public ResponseEntity<EvaluationResult> getEvaluation() {
        EvaluationResult eval = datasetTrainingService.getEvaluationResult();
        if (eval == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(eval);
    }
}
