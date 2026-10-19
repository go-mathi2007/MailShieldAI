package com.mailshield.controller;

import com.mailshield.dto.ScanStatisticsResponse;
import com.mailshield.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final HistoryService historyService;

    public StatisticsController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<ScanStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(historyService.getStatistics());
    }
}
