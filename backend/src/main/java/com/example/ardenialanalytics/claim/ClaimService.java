package com.example.ardenialanalytics.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;

    public ClaimService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public Page<Claim> findDeniedClaims(String payer, String reason, LocalDate startDate,
                                        LocalDate endDate, Pageable pageable) {
        Specification<Claim> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), "denied");

        if (payer != null && !payer.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("payer"), payer));
        }
        if (reason != null && !reason.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("denialReason"), reason));
        }
        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("deniedAt"), startDate));
        }
        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("deniedAt"), endDate));
        }

        return claimRepository.findAll(specification, pageable);
    }
}
