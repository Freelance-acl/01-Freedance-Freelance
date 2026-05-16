-- ============================================================================
-- Wallet Service S5 test datasets
-- Covers:
--   S5-F1 Get payouts by status/date range
--
-- Prerequisite:
--   Run root seed.sql first so contract_id=1 and freelancer_id=1 exist.
-- ============================================================================

-- ============================================================================
-- [S5-F1] Dataset: 5 payouts
--   - 2 COMPLETED in March
--   - 1 REFUNDED in March
--   - 2 COMPLETED in February
-- Expected:
--   GET /api/payouts/search?status=COMPLETED&startDate=2026-03-01&endDate=2026-03-31 -> 2
--   GET /api/payouts/search?startDate=2026-03-01&endDate=2026-03-31 -> 3
-- ============================================================================

DELETE FROM payouts WHERE id BETWEEN 5101 AND 5105;

INSERT INTO payouts
    (id, contract_id, freelancer_id, amount, method, status, transaction_details, created_at)
VALUES
    (5101, 1, 1, 1200.00, 'BANK_TRANSFER', 'COMPLETED',
     '{"gatewayResponse":"approved","ref":"S5F1-5101"}'::jsonb, '2026-03-05 10:00:00'),
    (5102, 1, 1, 1600.00, 'PAYPAL', 'COMPLETED',
     '{"gatewayResponse":"approved","ref":"S5F1-5102"}'::jsonb, '2026-03-28 16:20:00'),
    (5103, 1, 1, 900.00, 'CRYPTO', 'REFUNDED',
     '{"gatewayResponse":"approved","ref":"S5F1-5103"}'::jsonb, '2026-03-11 09:45:00'),
    (5104, 1, 1, 700.00, 'BANK_TRANSFER', 'COMPLETED',
     '{"gatewayResponse":"approved","ref":"S5F1-5104"}'::jsonb, '2026-02-08 12:30:00'),
    (5105, 1, 1, 1100.00, 'PAYPAL', 'COMPLETED',
     '{"gatewayResponse":"approved","ref":"S5F1-5105"}'::jsonb, '2026-02-21 14:10:00');

-- Optional check query:
-- SELECT id, status, created_at FROM payouts
-- WHERE id BETWEEN 5101 AND 5105
-- ORDER BY created_at DESC;
