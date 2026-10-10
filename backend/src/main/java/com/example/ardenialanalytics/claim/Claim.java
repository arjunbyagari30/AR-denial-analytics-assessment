package com.example.ardenialanalytics.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String payer;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String provider;

    @NotBlank
    @Column(name = "cpt_code", nullable = false, length = 10)
    private String cptCode;

    @NotNull
    @PositiveOrZero
    @Column(name = "billed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal billedAmount;

    @Column(name = "denial_reason", length = 255)
    private String denialReason;

    @Column(name = "denied_at")
    private LocalDate deniedAt;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status;

    protected Claim() {
    }

    public Long getId() { return id; }
    public String getPayer() { return payer; }
    public String getProvider() { return provider; }
    public String getCptCode() { return cptCode; }
    public BigDecimal getBilledAmount() { return billedAmount; }
    public String getDenialReason() { return denialReason; }
    public LocalDate getDeniedAt() { return deniedAt; }
    public String getStatus() { return status; }
}
