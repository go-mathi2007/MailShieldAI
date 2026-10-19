package com.mailshield.controller;

import com.mailshield.service.provider.EmailProviderService;
import com.mailshield.service.provider.ProviderStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final List<EmailProviderService> providers;

    public ProviderController(List<EmailProviderService> providers) {
        this.providers = providers;
    }

    @GetMapping
    public ResponseEntity<List<ProviderStatus>> getProviders() {
        List<ProviderStatus> list = providers.stream()
                .map(EmailProviderService::getStatus)
                .toList();
        return ResponseEntity.ok(list);
    }
}
