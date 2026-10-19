package com.mailshield.controller;

import com.mailshield.dto.EmailAnalysisRequest;
import com.mailshield.dto.EmailAnalysisResponse;
import com.mailshield.service.EmailAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyze")
public class AnalysisController {

    private final EmailAnalysisService analysisService;

    public AnalysisController(EmailAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<EmailAnalysisResponse> analyzePastedEmail(
            @Valid @RequestBody EmailAnalysisRequest request
    ) {
        EmailAnalysisResponse response = analysisService.analyzePastedEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<EmailAnalysisResponse> analyzeUploadedFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        EmailAnalysisResponse response = analysisService.analyzeUploadedFile(file);
        return ResponseEntity.ok(response);
    }
}
