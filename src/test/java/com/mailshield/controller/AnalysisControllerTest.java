package com.mailshield.controller;

import com.mailshield.dto.EmailAnalysisRequest;
import com.mailshield.dto.EmailAnalysisResponse;
import com.mailshield.model.PredictionResult;
import com.mailshield.service.EmailAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailAnalysisService analysisService;

    @Test
    @DisplayName("POST /api/analyze with valid payload returns 200 and result JSON")
    void testAnalyzePastedEmailSuccess() throws Exception {
        EmailAnalysisResponse mockResponse = new EmailAnalysisResponse(
                1L, Instant.now(), "sender@test.com", "Important Notice",
                "SPAM", 0.94, 0.06, -0.69, -0.69, -10.0, -18.0, -10.69, -18.69,
                12, 11, 1, Collections.emptyList(), Collections.emptyList(),
                "High spam confidence", PredictionResult.DISCLAIMER_TEXT,
                "PASTE", "abc123hash", 0, Collections.emptyList()
        );

        when(analysisService.analyzePastedEmail(any(EmailAnalysisRequest.class))).thenReturn(mockResponse);

        EmailAnalysisRequest req = new EmailAnalysisRequest(
                "sender@test.com", "Important Notice", "Urgent: please verify your account at once."
        );

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedLabel").value("SPAM"))
                .andExpect(jsonPath("$.spamProbability").value(0.94))
                .andExpect(jsonPath("$.disclaimer").exists());
    }

    @Test
    @DisplayName("POST /api/analyze with blank body returns 400 Bad Request")
    void testAnalyzePastedEmailValidationFailure() throws Exception {
        EmailAnalysisRequest req = new EmailAnalysisRequest("test@test.com", "Test", "   ");

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/analyze/upload with valid .eml file returns 200")
    void testUploadEmlSuccess() throws Exception {
        EmailAnalysisResponse mockResponse = new EmailAnalysisResponse(
                2L, Instant.now(), "office@corp.com", "Sprint Review",
                "HAM", 0.03, 0.97, -0.69, -0.69, -20.0, -8.0, -20.69, -8.69,
                8, 8, 0, Collections.emptyList(), Collections.emptyList(),
                "Legitimate email", PredictionResult.DISCLAIMER_TEXT,
                "FILE_UPLOAD", "def456hash", 0, Collections.emptyList()
        );

        when(analysisService.analyzeUploadedFile(any())).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.eml",
                "message/rfc822",
                "From: office@corp.com\nSubject: Sprint Review\n\nAgenda for sprint review".getBytes()
        );

        mockMvc.perform(multipart("/api/analyze/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedLabel").value("HAM"))
                .andExpect(jsonPath("$.hamProbability").value(0.97));
    }
}
