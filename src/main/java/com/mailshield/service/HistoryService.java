package com.mailshield.service;

import com.mailshield.dto.HistoryItemResponse;
import com.mailshield.dto.ScanStatisticsResponse;
import com.mailshield.model.ClassificationRecord;
import com.mailshield.model.EmailData;
import com.mailshield.model.PredictionResult;
import com.mailshield.repository.ClassificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Service managing scan history persistence and aggregate statistics.
 * In compliance with privacy guidelines, full email bodies are NEVER stored.
 */
@Service
public class HistoryService {

    private final ClassificationRepository repository;

    public HistoryService(ClassificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ClassificationRecord recordScan(EmailData emailData, PredictionResult prediction, String inputSource) {
        String hash = computeSha256(emailData.subject() + "\n" + emailData.body());
        String snippet = createSafeSnippet(emailData.body());

        ClassificationRecord record = new ClassificationRecord(
                Instant.now(),
                truncate(emailData.subject(), 500),
                truncate(emailData.sender(), 255),
                prediction.predictedLabel(),
                prediction.spamProbability(),
                prediction.hamProbability(),
                hash,
                inputSource,
                snippet,
                truncate(prediction.explanation(), 500),
                emailData.attachmentCount()
        );

        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<HistoryItemResponse> getRecentHistory() {
        return repository.findAllByOrderByTimestampDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScanStatisticsResponse getStatistics() {
        long total = repository.count();
        long spam = repository.countByPredictedLabelIgnoreCase("SPAM");
        long ham = repository.countByPredictedLabelIgnoreCase("HAM");

        double spamPct = total == 0 ? 0.0 : ((double) spam / total) * 100.0;
        double hamPct = total == 0 ? 0.0 : ((double) ham / total) * 100.0;

        return new ScanStatisticsResponse(
                total,
                spam,
                ham,
                round(spamPct, 1),
                round(hamPct, 1)
        );
    }

    @Transactional
    public void clearHistory() {
        repository.deleteAll();
    }

    private HistoryItemResponse toDto(ClassificationRecord rec) {
        return new HistoryItemResponse(
                rec.getId(),
                rec.getTimestamp(),
                rec.getSubject(),
                rec.getSender(),
                rec.getPredictedLabel(),
                rec.getSpamProbability(),
                rec.getHamProbability(),
                rec.getContentHash(),
                rec.getInputSource(),
                rec.getSnippet(),
                rec.getExplanation(),
                rec.getAttachmentCount() != null ? rec.getAttachmentCount() : 0
        );
    }

    private String computeSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return "sha256_unavailable";
        }
    }

    private String createSafeSnippet(String body) {
        if (body == null || body.isBlank()) return "";
        String clean = body.replaceAll("\\s+", " ").trim();
        return truncate(clean, 120);
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        if (text.length() <= max) return text;
        return text.substring(0, max);
    }

    private static double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
