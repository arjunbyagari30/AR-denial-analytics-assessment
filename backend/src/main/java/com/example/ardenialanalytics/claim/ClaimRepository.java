package com.example.ardenialanalytics.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    @Query("""
            SELECT new com.example.ardenialanalytics.analytics.PayerAnalytics(
                c.payer, SUM(c.billedAmount), COUNT(c))
            FROM Claim c
            WHERE c.status = 'denied'
            GROUP BY c.payer
            ORDER BY SUM(c.billedAmount) DESC, c.payer ASC
            """)
    List<com.example.ardenialanalytics.analytics.PayerAnalytics> findPayerAnalytics();

    @Query("""
            SELECT new com.example.ardenialanalytics.analytics.ReasonAnalytics(
                c.denialReason, SUM(c.billedAmount))
            FROM Claim c
            WHERE c.status = 'denied' AND c.denialReason IS NOT NULL
            GROUP BY c.denialReason
            ORDER BY SUM(c.billedAmount) DESC, c.denialReason ASC
            """)
    List<com.example.ardenialanalytics.analytics.ReasonAnalytics> findReasonAnalytics();
}
