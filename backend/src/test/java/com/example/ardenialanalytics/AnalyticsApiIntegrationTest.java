package com.example.ardenialanalytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpClaims() {
        jdbcTemplate.update("DELETE FROM claims");
        insertClaim(1, "Alpha Health", "Clinic One", "99213", "100.00", "Authorization missing", "2025-01-10", "denied");
        insertClaim(2, "Alpha Health", "Clinic One", "99214", "50.00", "Authorization missing", "2025-01-11", "denied");
        insertClaim(3, "Beta Assurance", "Clinic Two", "93000", "200.00", "Coding mismatch", "2025-01-12", "denied");
        insertClaim(4, "Alpha Health", "Clinic One", "80053", "500.00", null, null, "paid");
        insertClaim(5, "Beta Assurance", "Clinic Two", "71046", "25.00", "Authorization missing", "2025-02-01", "denied");
        insertClaim(6, "Gamma Mutual", "Clinic Three", "36415", "75.00", "Coding mismatch", "2025-02-02", "denied");
    }

    @Test
    void payerAnalyticsSumAndCountOnlyDeniedClaimsInDescendingOrder() throws Exception {
        mockMvc.perform(get("/api/analytics/payers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].payer").value("Beta Assurance"))
                .andExpect(jsonPath("$[0].totalDeniedAmount").value(225.0))
                .andExpect(jsonPath("$[0].deniedClaimCount").value(2))
                .andExpect(jsonPath("$[1].payer").value("Alpha Health"))
                .andExpect(jsonPath("$[1].totalDeniedAmount").value(150.0))
                .andExpect(jsonPath("$[1].deniedClaimCount").value(2))
                .andExpect(jsonPath("$[2].payer").value("Gamma Mutual"))
                .andExpect(jsonPath("$[2].totalDeniedAmount").value(75.0));
    }

    @Test
    void reasonAnalyticsGroupsDeniedBilledAmountsAndExcludesPaidClaims() throws Exception {
        mockMvc.perform(get("/api/analytics/reasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].reason").value("Coding mismatch"))
                .andExpect(jsonPath("$[0].totalDeniedAmount").value(275.0))
                .andExpect(jsonPath("$[1].reason").value("Authorization missing"))
                .andExpect(jsonPath("$[1].totalDeniedAmount").value(175.0));
    }

    @Test
    void claimsCanBeFilteredByPayerReasonAndInclusiveDateRange() throws Exception {
        mockMvc.perform(get("/api/claims")
                        .param("payer", "Alpha Health")
                        .param("reason", "Authorization missing")
                        .param("startDate", "2025-01-11")
                        .param("endDate", "2025-01-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].status").value("denied"));
    }

    @Test
    void claimsPaginationReturnsExpectedContentAndMetadata() throws Exception {
        mockMvc.perform(get("/api/claims").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[1].id").value(2));
    }

    @Test
    void invalidDateRangeReturnsHttp400JsonError() throws Exception {
        mockMvc.perform(get("/api/claims").param("startDate", "2025-02-02").param("endDate", "2025-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATE_RANGE"))
                .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));
    }

    @Test
    void invalidDateFormatAndPaginationValuesReturnHttp400JsonErrors() throws Exception {
        mockMvc.perform(get("/api/claims").param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATE_FORMAT"))
                .andExpect(jsonPath("$.message", containsString("YYYY-MM-DD")));

        for (String[] invalidParameter : new String[][]{{"page", "-1"}, {"size", "0"}, {"size", "101"}}) {
            mockMvc.perform(get("/api/claims").param(invalidParameter[0], invalidParameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_QUERY_PARAMETER"));
        }
    }

    private void insertClaim(long id, String payer, String provider, String cptCode,
                             String amount, String reason, String deniedAt, String status) {
        LocalDate denialDate = deniedAt == null ? null : LocalDate.parse(deniedAt);
        jdbcTemplate.update("""
                        INSERT INTO claims (id, payer, provider, cpt_code, billed_amount, denial_reason, denied_at, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, payer, provider, cptCode, new BigDecimal(amount), reason,
                denialDate == null ? null : Date.valueOf(denialDate), status);
    }
}
