package com.example.ardenialanalytics.claim;

import java.util.List;

public record ClaimPageResponse(
        List<Claim> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
