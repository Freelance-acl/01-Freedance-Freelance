-- ============================================================================
-- Seed data for [S5-F3] Freelancer Payout Summary
--   GET /api/payouts/freelancer/{freelancerId}/summary
--
-- Test scenario:
--   a) A freelancer (id=200) exists with 4 COMPLETED payouts:
--        2 BANK_TRANSFER  (1500, 2000)  -> 3500
--        1 PAYPAL          (800)
--        1 CRYPTO          (500)
--   b) GET /api/payouts/freelancer/200/summary
--        -> totalPayouts=4
--        -> totalAmount=4800
--        -> methodBreakdown={BANK_TRANSFER:3500, PAYPAL:800, CRYPTO:500}
--   c) GET /api/payouts/freelancer/9999/summary  -> 404
--
-- We use a fresh test freelancer (id=200) instead of Ahmed (id=1) so the
-- existing payout #1 (10K) doesn't leak into the assertion.
--
-- Notes on FKs:
--   * payouts.freelancer_id and payouts.contract_id are plain BIGINT columns
--     (no DB-level FK constraint), so we can use any values that semantically
--     make sense — here we link payouts to the existing contract id=1 from
--     seed.sql for traceability.
-- ============================================================================

-- Test freelancer for this feature -------------------------------------------
INSERT INTO users
    (id, name, email, password, phone, role, status, preferences, created_at)
VALUES
    (200, 'Test Freelancer', 's5f3.freelancer@mail.com', 'pass123',
     '+201099999200', 'FREELANCER', 'ACTIVE',
     '{
        "language": "en",
        "notifications": {"email": true, "sms": false},
        "timezone": "Africa/Cairo"
     }'::jsonb,
     '2026-04-01 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 4 COMPLETED payouts for the test freelancer --------------------------------
INSERT INTO payouts
    (id, contract_id, freelancer_id, amount, method, status,
     transaction_details, created_at)
VALUES
    (200, 1, 200, 1500, 'BANK_TRANSFER', 'COMPLETED',
     '{"gatewayResponse":"approved","accountLastFour":"1111"}'::jsonb,
     '2026-04-02 10:00:00'),

    (201, 1, 200, 2000, 'BANK_TRANSFER', 'COMPLETED',
     '{"gatewayResponse":"approved","accountLastFour":"2222"}'::jsonb,
     '2026-04-03 10:00:00'),

    (202, 1, 200,  800, 'PAYPAL',        'COMPLETED',
     '{"gatewayResponse":"approved","paypalEmail":"freelancer@example.com"}'::jsonb,
     '2026-04-04 10:00:00'),

    (203, 1, 200,  500, 'CRYPTO',        'COMPLETED',
     '{"gatewayResponse":"approved","walletAddress":"0xABCDEF"}'::jsonb,
     '2026-04-05 10:00:00');

-- Re-sync IDENTITY sequences for the affected tables ------------------------
SELECT setval(
    pg_get_serial_sequence('users', 'id'),
    COALESCE((SELECT MAX(id) FROM users), 1)
);
SELECT setval(
    pg_get_serial_sequence('payouts', 'id'),
    COALESCE((SELECT MAX(id) FROM payouts), 1)
);
