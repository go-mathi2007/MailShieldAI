package com.mailshield.controller;

import com.mailshield.dto.HistoryItemResponse;
import com.mailshield.dto.ScanStatisticsResponse;
import com.mailshield.service.HistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HistoryController.class, StatisticsController.class})
class HistoryAndStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HistoryService historyService;

    @Test
    @DisplayName("GET /api/history returns recent scan list")
    void testGetHistory() throws Exception {
        HistoryItemResponse item = new HistoryItemResponse(
                1L, Instant.now(), "Lottery Winner", "winner@prize.com",
                "SPAM", 0.99, 0.01, "hash123", "PASTE", "Congratulations winner", "High confidence", 0
        );

        when(historyService.getRecentHistory()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].predictedLabel").value("SPAM"))
                .andExpect(jsonPath("$[0].subject").value("Lottery Winner"));
    }

    @Test
    @DisplayName("DELETE /api/history returns confirmation message")
    void testClearHistory() throws Exception {
        mockMvc.perform(delete("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Classification history cleared successfully."));
    }

    @Test
    @DisplayName("GET /api/statistics returns accurate aggregated metrics")
    void testGetStatistics() throws Exception {
        ScanStatisticsResponse stats = new ScanStatisticsResponse(50, 30, 20, 60.0, 40.0);
        when(historyService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(50))
                .andExpect(jsonPath("$.spamCount").value(30))
                .andExpect(jsonPath("$.hamCount").value(20))
                .andExpect(jsonPath("$.spamPercentage").value(60.0))
                .andExpect(jsonPath("$.hamPercentage").value(40.0));
    }
}
