package com.mailshield.service;

import com.mailshield.dto.EmailAnalysisRequest;
import com.mailshield.dto.EmailAnalysisResponse;
import com.mailshield.model.ClassificationRecord;
import com.mailshield.model.EmailData;
import com.mailshield.model.PredictionResult;
import com.mailshield.ml.NaiveBayesClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailAnalysisServiceTest {

    private NaiveBayesClassifier classifier;
    private EmlParserService emlParserService;
    private HistoryService historyService;
    private EmailAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        classifier = Mockito.mock(NaiveBayesClassifier.class);
        emlParserService = Mockito.mock(EmlParserService.class);
        historyService = Mockito.mock(HistoryService.class);
        analysisService = new EmailAnalysisService(classifier, emlParserService, historyService);
    }

    @Test
    @DisplayName("Should reject empty pasted email body")
    void testEmptyPasteThrows() {
        EmailAnalysisRequest req = new EmailAnalysisRequest("test@test.com", "Test Subject", "   ");
        assertThrows(IllegalArgumentException.class, () -> analysisService.analyzePastedEmail(req));
    }

    @Test
    @DisplayName("Should reject non-eml file upload")
    void testNonEmlFileUploadRejected() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "MZ...".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> analysisService.analyzeUploadedFile(exeFile));
    }

    @Test
    @DisplayName("Should reject empty file upload")
    void testEmptyFileUploadRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.eml",
                "message/rfc822",
                new byte[0]
        );
        assertThrows(IllegalArgumentException.class, () -> analysisService.analyzeUploadedFile(emptyFile));
    }

    @Test
    @DisplayName("Should parse and classify valid .eml file")
    void testValidEmlUpload() throws Exception {
        byte[] emlBytes = "From: bob@test.com\nSubject: Hi\n\nMeeting at 10".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.eml", "message/rfc822", emlBytes);

        EmailData parsed = new EmailData("bob@test.com", "Hi", "Meeting at 10", null, "text/plain", false, 0, List.of());
        when(emlParserService.parseEml(any())).thenReturn(parsed);

        ClassificationRecord mockRecord = new ClassificationRecord();
        mockRecord.setId(10L);
        mockRecord.setPredictedLabel("HAM");
        when(historyService.recordScan(any(), any(), any())).thenReturn(mockRecord);

        PredictionResult pred = new PredictionResult(
                "HAM", 0.05, 0.95, -0.69, -0.69, -5.0, -2.0, -5.69, -2.69, 5, 5, 0,
                Collections.emptyList(), Collections.emptyList(), "Legitimate", PredictionResult.DISCLAIMER_TEXT
        );
        when(classifier.classify("Hi", "Meeting at 10")).thenReturn(pred);

        EmailAnalysisResponse res = analysisService.analyzeUploadedFile(file);

        assertNotNull(res);
        assertEquals("HAM", res.predictedLabel());
        assertEquals(0.95, res.hamProbability());
        verify(historyService, times(1)).recordScan(any(), any(), eq("FILE_UPLOAD"));
    }
}
