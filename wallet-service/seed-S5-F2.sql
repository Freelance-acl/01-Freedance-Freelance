--  docker exec -i freelance-db psql -U postgres -d freelancedb < seed-S5-F2.sql

-- ============================================================================
-- Seed data for [S5-F2] Process Refund
--   PUT /api/payouts/{id}/refund   body: {"reason":"<text>"}
--
-- Test scenario covered:
--   a) A COMPLETED payout exists  (id = 100) -> happy path + retry test
--   b) PUT  /api/payouts/100/refund          -> 200, status=REFUNDED
--   c) PUT  /api/payouts/100/refund (again)  -> 400 (already REFUNDED)
--   d) PUT  /api/payouts/101/refund          -> 400 (not COMPLETED, FAILED)
--
-- Prerequisite: seed.sql has been run, so contract id=1 and user id=1 exist.
-- IDs 100/101 are used (instead of low numbers) to avoid colliding with any
-- payouts created by previous seeds or by other features.
-- ============================================================================

INSERT INTO payouts
    (id, contract_id, freelancer_id, amount, method, status,
     transaction_details, created_at)
VALUES
    -- (a) COMPLETED payout to refund (tests b and c)
    (100, 1, 1, 5000, 'BANK_TRANSFER', 'COMPLETED',
     '{
        "gatewayResponse": "approved",
        "accountLastFour": "4242",
        "receiptUrl": "https://pay.example/payout100",
        "failureReason": null
     }'::jsonb,
     '2026-04-01 10:00:00'),

    -- (d) FAILED payout to confirm refund is rejected with 400
    (101, 1, 1, 1500, 'PAYPAL', 'FAILED',
     '{
        "gatewayResponse": "rejected",
        "accountLastFour": "1111",
        "retryAttempt": 0,
        "failureReason": "insufficient funds"
     }'::jsonb,
     '2026-04-02 11:30:00');

-- Re-sync the IDENTITY sequence so future auto-generated payout IDs
-- start above 101 and don't collide with the rows inserted above.
SELECT setval(
    pg_get_serial_sequence('payouts', 'id'),
    COALESCE((SELECT MAX(id) FROM payouts), 1)
);
