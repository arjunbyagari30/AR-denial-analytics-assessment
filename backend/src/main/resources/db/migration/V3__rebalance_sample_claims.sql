-- Spread fictional claims across payer and denial-reason groups for useful assessments.
UPDATE claims
SET payer = CASE MOD(id + FLOOR(id / 5), 5)
        WHEN 0 THEN 'Northstar Health Plan'
        WHEN 1 THEN 'Summit Mutual'
        WHEN 2 THEN 'Evergreen Assurance'
        WHEN 3 THEN 'HarborPoint Insurance'
        ELSE 'Pioneer Benefits'
    END,
    denial_reason = CASE
        WHEN status = 'denied' THEN CASE MOD(id * 2 + FLOOR(id / 7), 5)
            WHEN 0 THEN 'Authorization missing'
            WHEN 1 THEN 'Coding mismatch'
            WHEN 2 THEN 'Coverage inactive'
            WHEN 3 THEN 'Duplicate claim'
            ELSE 'Service not covered'
        END
        ELSE NULL
    END;
