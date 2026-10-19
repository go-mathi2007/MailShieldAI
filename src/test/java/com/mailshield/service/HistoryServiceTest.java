package com.mailshield.service;

import com.mailshield.dto.HistoryItemResponse;
import com.mailshield.dto.ScanStatisticsResponse;
import com.mailshield.model.ClassificationRecord;
import com.mailshield.model.EmailData;
import com.mailshield.model.PredictionResult;
import com.mailshield.repository.ClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HistoryServiceTest {

    private ClassificationRepository repository;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ClassificationRepository.class);
        historyService = new HistoryService(repository);
    }

    @Test
    @DisplayName("Should save classification record without storing full body")
    void testRecordScanPrivacy() {
        EmailData emailData = new EmailData(
                "phish@scam.com",
                "Immediate Account Verification Required",
                "Dear customer, your bank credentials have expired. Enter your password now to restore access.",
                "2026-10-19",
                "text/plain",
                false,
                0,
                List.of()
        );

        PredictionResult pred = new PredictionResult(
                "SPAM",
                0.985,
                0.015,
                -0.69,
                -0.69,
                -12.4,
                -24.8,
                -13.09,
                -25.49,
                15,
                14,
                1,
                Collections.emptyList(),
                Collections.emptyList(),
                "High probability spam",
                PredictionResult.DISCLAIMER_TEXT
        );

        when(repository.save(any(ClassificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClassificationRecord saved = historyService.recordScan(emailData, pred, "PASTE");

        assertNotNull(saved);
        assertEquals("SPAM", saved.getPredictedLabel());
        assertEquals("Immediate Account Verification Required", saved.getSubject());
        assertNotNull(saved.getContentHash());
        assertEquals(64, saved.getContentHash().length()); // SHA-256 length
        assertTrue(saved.getSnippet().startsWith("Dear customer, your bank credentials"));

        // Verify snippet is truncated and repository was called
        verify(repository, times(1)).save(any(ClassificationRecord.class));
    }

    @Test
    @DisplayName("Should compute correct statistics from repository counts")
    void testStatisticsComputation() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByPredictedLabelIgnoreCase("SPAM")).thenReturn(7L);
        when(repository.countByPredictedLabelIgnoreCase("HAM")).thenReturn(3L);

        ScanStatisticsResponse stats = historyService.getStatistics();

        assertEquals(10L, stats.totalScans());
        assertEquals(7L, stats.spamCount());
        assertEquals(3L, stats.hamCount());
        assertEquals(70.0, stats.spamPercentage());
        assertEquals(30.0, stats.hamPercentage());
    }

    @Test
    @DisplayName("Should clear history on delete request")
    void testClearHistory() {
        historyService.clearHistory();
        verify(repository, times(1)).deleteAll();
    }
}
