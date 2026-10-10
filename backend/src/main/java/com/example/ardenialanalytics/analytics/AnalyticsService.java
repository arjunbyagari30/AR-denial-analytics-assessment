package com.example.ardenialanalytics.analytics;

import com.example.ardenialanalytics.claim.ClaimRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final ClaimRepository claimRepository;

    public AnalyticsService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public List<PayerAnalytics> findPayerAnalytics() {
        return claimRepository.findPayerAnalytics();
    }

    public List<ReasonAnalytics> findReasonAnalytics() {
        return claimRepository.findReasonAnalytics();
    }
}
