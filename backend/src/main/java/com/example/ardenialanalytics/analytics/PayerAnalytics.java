package com.example.ardenialanalytics.analytics;

import java.math.BigDecimal;

public record PayerAnalytics(String payer, BigDecimal totalDeniedAmount, long deniedClaimCount) {
}
