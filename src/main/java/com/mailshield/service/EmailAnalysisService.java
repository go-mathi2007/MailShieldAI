package com.mailshield.service;

import com.mailshield.dto.EmailAnalysisRequest;
import com.mailshield.dto.EmailAnalysisResponse;
import com.mailshield.model.ClassificationRecord;
import com.mailshield.model.EmailData;
import com.mailshield.model.PredictionResult;
import com.mailshield.ml.NaiveBayesClassifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class EmailAnalysisService {

    private final NaiveBayesClassifier classifier;
    private final EmlParserService emlParserService;
    private final HistoryService historyService;

    public EmailAnalysisService(
            NaiveBayesClassifier classifier,
            EmlParserService emlParserService,
            HistoryService historyService
    ) {
        this.classifier = classifier;
        this.emlParserService = emlParserService;
        this.historyService = historyService;
    }

    public EmailAnalysisResponse analyzePastedEmail(EmailAnalysisRequest request) {
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Email body cannot be empty.");
        }

        EmailData emailData = EmailData.fromPasted(request.getSender(), request.getSubject(), request.getBody());
        PredictionResult prediction = classifier.classify(emailData.subject(), emailData.body());

        ClassificationRecord saved = historyService.recordScan(emailData, prediction, "PASTE");

        return toResponse(saved, prediction, emailData.attachmentNames());
    }

    public EmailAnalysisResponse analyzeUploadedFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded or the file is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase().endsWith(".eml")) {
            throw new IllegalArgumentException("Invalid file type: '" + originalFilename + "'. Only .eml RFC-822 email files are accepted.");
        }

        // Limit file size to 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Uploaded file exceeds 5MB size limit.");
        }

        EmailData emailData = emlParserService.parseEml(file.getBytes());

        if (emailData.body().isBlank() && emailData.subject().isBlank()) {
            throw new IllegalArgumentException("Uploaded .eml file contains no extractable subject or body text.");
        }

        PredictionResult prediction = classifier.classify(emailData.subject(), emailData.body());
        ClassificationRecord saved = historyService.recordScan(emailData, prediction, "FILE_UPLOAD");

        return toResponse(saved, prediction, emailData.attachmentNames());
    }

    private EmailAnalysisResponse toResponse(
            ClassificationRecord record,
            PredictionResult prediction,
            List<String> attachmentNames
    ) {
        return new EmailAnalysisResponse(
                record.getId(),
                record.getTimestamp(),
                record.getSender(),
                record.getSubject(),
                prediction.predictedLabel(),
                prediction.spamProbability(),
                prediction.hamProbability(),
                prediction.logPriorSpam(),
                prediction.logPriorHam(),
                prediction.logLikelihoodSpam(),
                prediction.logLikelihoodHam(),
                prediction.totalLogScoreSpam(),
                prediction.totalLogScoreHam(),
                prediction.totalTokens(),
                prediction.recognizedTokens(),
                prediction.unknownTokens(),
                prediction.topSpamIndicators(),
                prediction.topHamIndicators(),
                prediction.explanation(),
                prediction.disclaimer(),
                record.getInputSource(),
                record.getContentHash(),
                record.getAttachmentCount() != null ? record.getAttachmentCount() : 0,
                attachmentNames != null ? attachmentNames : List.of()
        );
    }
}
