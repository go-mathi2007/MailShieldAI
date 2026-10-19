package com.mailshield.controller;

import com.mailshield.dto.HistoryItemResponse;
import com.mailshield.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<List<HistoryItemResponse>> getRecentHistory() {
        return ResponseEntity.ok(historyService.getRecentHistory());
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearHistory() {
        historyService.clearHistory();
        return ResponseEntity.ok(Map.of("message", "Classification history cleared successfully."));
    }
}
