package com.example.ardenialanalytics.analytics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/payers")
    public List<PayerAnalytics> getPayerAnalytics() {
        return analyticsService.findPayerAnalytics();
    }

    @GetMapping("/reasons")
    public List<ReasonAnalytics> getReasonAnalytics() {
        return analyticsService.findReasonAnalytics();
    }
}
