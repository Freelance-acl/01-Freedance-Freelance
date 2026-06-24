-- docker exec -i freelance-db psql -U postgres -d freelancedb < seed.sql

-- ============================================================================
-- Freelance Marketplace - Database Seed
-- Source: Milestone 1 PDF, Section 15 "Appendix: Example Database Tables"
-- ----------------------------------------------------------------------------
-- M2 auth: passwords are BCrypt hashes ($2a$ / $2b$ / $2y$, 60 chars).
-- Plaintext login password for all users below: securePassword123
--   POST /api/auth/login  { "email": "...", "password": "securePassword123" }
-- ============================================================================

-- ============================================================================
-- 15.1.1  users
-- ============================================================================
INSERT INTO users
    (id, name, email, password, phone, role, status, preferences, created_at)
VALUES
    (1, 'Ahmed Hassan', 'ahmed@mail.com',
     '$2a$10$.F.hAy2H0GbGYDuhvLYeZeyQ5j8bgncylLVySmYHITqFld1Uedpvq',
     '+201012345601', 'FREELANCER', 'ACTIVE',
     '{
        "language": "ar",
        "notifications": {"email": true, "sms": false},
        "timezone": "Africa/Cairo",
        "profileVisibility": "PUBLIC",
        "hourlyRateRange": {"min": 300, "max": 600}
     }'::jsonb,
     '2026-03-01 10:00:00'),

    (2, 'Sara Mohamed', 'sara@mail.com',
     '$2a$10$.F.hAy2H0GbGYDuhvLYeZeyQ5j8bgncylLVySmYHITqFld1Uedpvq',
     '+201012345602', 'CLIENT', 'ACTIVE',
     '{
        "language": "en",
        "notifications": {"email": true, "sms": true},
        "timezone": "Africa/Cairo",
        "profileVisibility": "PUBLIC"
     }'::jsonb,
     '2026-03-01 10:01:00'),

    (3, 'Admin User', 'admin@freelance.com',
     '$2a$10$.F.hAy2H0GbGYDuhvLYeZeyQ5j8bgncylLVySmYHITqFld1Uedpvq',
     '+201012345603', 'ADMIN', 'ACTIVE',
     '{
        "language": "en",
        "notifications": {"email": true, "sms": false},
        "timezone": "UTC",
        "profileVisibility": "PRIVATE"
     }'::jsonb,
     '2026-03-01 10:02:00')
ON CONFLICT (id) DO UPDATE SET
    name        = EXCLUDED.name,
    email       = EXCLUDED.email,
    password    = EXCLUDED.password,
    phone       = EXCLUDED.phone,
    role        = EXCLUDED.role,
    status      = EXCLUDED.status,
    preferences = EXCLUDED.preferences,
    created_at  = EXCLUDED.created_at;

-- ============================================================================
-- 15.1.2  user_skills  (Ahmed's skills)
-- ============================================================================
INSERT INTO user_skills
    (id, user_id, skill_name, category, years_of_experience, proficiency_level, is_primary, metadata, created_at)
VALUES
    (1, 1, 'Java',        'DEVELOPMENT', 5, 'EXPERT',       TRUE,
     '{
        "certifications": ["Oracle Certified Professional"],
        "portfolioUrls": ["https://github.com/ahmed-dev"],
        "endorsementCount": 12,
        "lastUsedDate": "2026-03-10",
        "tools": ["IntelliJ IDEA", "Maven", "Docker"]
     }'::jsonb,
     '2026-03-01 10:05:00'),

    (2, 1, 'Spring Boot', 'DEVELOPMENT', 3, 'INTERMEDIATE', FALSE,
     '{
        "certifications": [],
        "portfolioUrls": ["https://github.com/ahmed-dev/spring-projects"],
        "endorsementCount": 7,
        "lastUsedDate": "2026-03-10",
        "tools": ["Spring Data JPA", "Spring Security"]
     }'::jsonb,
     '2026-03-01 10:06:00'),

    (3, 1, 'React',       'DEVELOPMENT', 2, 'INTERMEDIATE', FALSE,
     '{
        "certifications": [],
        "portfolioUrls": ["https://github.com/ahmed-dev/react-portfolio"],
        "endorsementCount": 4,
        "lastUsedDate": "2026-02-20",
        "tools": ["Vite", "Redux Toolkit"]
     }'::jsonb,
     '2026-03-01 10:07:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.2.1  jobs
-- ============================================================================
INSERT INTO jobs
    (id, client_id, title, description, category, status,
     budget_min, budget_max, rating, total_ratings, requirements, created_at)
VALUES
    (1, 2, 'E-Commerce Backend',
     'Build a scalable e-commerce backend using Spring Boot, PostgreSQL and a microservices-friendly architecture. Includes catalog, orders, and payment integration.',
     'WEB_DEV', 'CLOSED', 5000, 15000, 4.8, 1,
     '{
        "requiredSkills": ["Java", "Spring Boot", "PostgreSQL"],
        "experienceLevel": "MID",
        "estimatedDuration": 8,
        "remoteAllowed": true,
        "preferredTimezone": "Africa/Cairo"
     }'::jsonb,
     '2026-03-01 11:00:00'),

    (2, 2, 'Mobile App UI',
     'Design a clean, modern mobile UI for an iOS/Android shopping companion app. Deliverables include Figma mockups, design system, and clickable prototype.',
     'DESIGN', 'OPEN', 3000, 8000, 0.0, 0,
     '{
        "requiredSkills": ["Figma", "UI/UX", "Mobile Design"],
        "experienceLevel": "JUNIOR",
        "estimatedDuration": 4,
        "remoteAllowed": true,
        "preferredTimezone": null
     }'::jsonb,
     '2026-03-15 09:30:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.2.2  job_attachments
-- ============================================================================
INSERT INTO job_attachments
    (id, job_id, type, file_url, expiry_date, verified, metadata, uploaded_at)
VALUES
    (1, 1, 'BRIEF',     '/docs/ecomm-brief.pdf', DATE '2026-12-31', TRUE,
     '{
        "fileSize": 245,
        "fileFormat": "PDF",
        "versionNumber": 2,
        "verificationNotes": "Verified by admin on 2026-03-05"
     }'::jsonb,
     '2026-03-01 11:05:00'),

    (2, 1, 'REFERENCE', '/docs/api-spec.pdf',    DATE '2026-09-30', TRUE,
     '{
        "fileSize": 180,
        "fileFormat": "PDF",
        "versionNumber": 1,
        "verificationNotes": "Verified by admin on 2026-03-05"
     }'::jsonb,
     '2026-03-01 11:10:00'),

    (3, 2, 'MOCKUP',    '/docs/app-mockup.fig',  DATE '2026-06-30', FALSE,
     '{
        "fileSize": 1024,
        "fileFormat": "FIG",
        "versionNumber": 1,
        "rejectionReason": null
     }'::jsonb,
     '2026-03-15 09:35:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.3.1  proposals
-- ============================================================================
INSERT INTO proposals
    (id, job_id, freelancer_id, cover_letter, bid_amount, estimated_days,
     status, metadata, submitted_at, accepted_at)
VALUES
    (1, 1, 1,
     'Microservices with Spring Boot. 5 years exp, 3 backends delivered.',
     10000, 45, 'ACCEPTED',
     '{"approachSummary":"Microservices with Spring Boot","relevantExperience":"3 e-commerce backends","toolsProposed":["Java 25","Spring Boot"],"availabilityStart":"2026-03-10"}',
     '2026-03-05 09:00:00',
     '2026-03-09 16:30:00'),

    (2, 2, 1,
     'Mobile-first Figma UI kit and prototype within timeline.',
     5000, 20, 'SUBMITTED',
     '{"approachSummary":"Mobile-first design","relevantExperience":"2 shipped mobile apps","toolsProposed":["Figma","Adobe XD"],"availabilityStart":"2026-03-25"}',
     '2026-03-18 14:00:00',
     NULL)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.3.2  proposal_milestones  (3 milestones for proposal #1)
-- ============================================================================
INSERT INTO proposal_milestones
    (id, proposal_id, milestone_order, title, description, amount, status, metadata)
VALUES
    (1, 1, 1, 'Database Design',
     'Design the relational schema, write migration scripts and seed data for development.',
     2000, 'COMPLETED',
     '{
        "deliverables": ["ER diagram", "Migration scripts", "Seed data"],
        "acceptanceCriteria": "All tables created with proper indexes",
        "estimatedDays": 7,
        "actualCompletionDate": "2026-03-17",
        "revisionCount": 1
     }'::jsonb),

    (2, 1, 2, 'API Development',
     'Implement REST API endpoints for catalog, cart, orders and payments using Spring Boot.',
     5000, 'COMPLETED',
     '{
        "deliverables": ["Catalog API", "Order API", "Payment API"],
        "acceptanceCriteria": "All endpoints documented and unit tested",
        "estimatedDays": 21,
        "actualCompletionDate": "2026-03-22",
        "revisionCount": 0
     }'::jsonb),

    (3, 1, 3, 'Testing & Deployment',
     'Add integration tests, finalize Docker Compose setup and deploy to a staging environment.',
     3000, 'COMPLETED',
     '{
        "deliverables": ["Unit tests", "Integration tests", "Docker Compose"],
        "acceptanceCriteria": "80% code coverage, all endpoints tested",
        "estimatedDays": 14,
        "actualCompletionDate": "2026-03-25",
        "revisionCount": 0
     }'::jsonb)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.4.1  contracts
-- ============================================================================
INSERT INTO contracts
    (id, job_id, freelancer_id, client_id, proposal_id, agreed_amount,
     status, start_date, end_date, metadata, created_at)
VALUES
    (1, 1, 1, 2, 1, 10000, 'COMPLETED',
     '2026-03-10 00:00:00', '2026-03-25 00:00:00',
     '{
        "paymentTerms": "MILESTONE",
        "revisionLimit": 3,
        "ndaSigned": true,
        "weeklyHoursExpected": 30,
        "progressPercentage": 100,
        "lastActivityDate": "2026-03-25"
     }'::jsonb,
     '2026-03-10 00:00:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.5.1  payouts
-- ============================================================================
INSERT INTO payouts
    (id, contract_id, freelancer_id, amount, method, status,
     transaction_details, created_at)
VALUES
    (1, 1, 1, 10000, 'BANK_TRANSFER', 'COMPLETED',
     '{
        "gatewayResponse": "approved",
        "accountLastFour": "9876",
        "receiptUrl": "https://pay.example/payout001",
        "failureReason": null
     }'::jsonb,
     '2026-03-25 17:50:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.5.2  promo_codes
-- ============================================================================
INSERT INTO promo_codes
    (id, code, discount_type, discount_value, max_uses, current_uses,
     expiry_date, active, metadata)
VALUES
    (1, 'FIRSTJOB20', 'PERCENTAGE', 20.0, 50, 1,
     '2026-09-30 23:59:00', TRUE,
     '{
        "eligibleJobCategories": ["WEB_DEV", "MOBILE", "DESIGN"],
        "minimumContractAmount": 1000,
        "termsAndConditions": "First completed contract only"
     }'::jsonb),

    (2, 'FLAT200',    'FIXED',      200.0, 100, 0,
     '2026-12-31 23:59:00', TRUE,
     '{
        "eligibleJobCategories": ["WEB_DEV"],
        "minimumContractAmount": 5000,
        "applicableRegions": ["Egypt", "UAE"]
     }'::jsonb),

    (3, 'LAUNCH10',   'PERCENTAGE', 10.0, 30, 30,
     '2026-02-28 23:59:00', FALSE,
     '{
        "eligibleJobCategories": ["WEB_DEV", "MOBILE", "DESIGN", "WRITING"],
        "minimumContractAmount": 0,
        "termsAndConditions": "Platform launch promo - expired"
     }'::jsonb)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 15.5.3  payout_promos  (Join entity: Payout #1 used FIRSTJOB20)
-- ============================================================================
INSERT INTO payout_promos
    (id, payout_id, promo_code_id, discount_applied, applied_at)
VALUES
    (1, 1, 1, 2000.00, '2026-03-25 17:55:00')
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- Re-sync IDENTITY sequences so future auto-generated IDs don't collide
-- with the explicit IDs inserted above.
-- ============================================================================
SELECT setval(pg_get_serial_sequence('users',                'id'), COALESCE((SELECT MAX(id) FROM users),                1));
SELECT setval(pg_get_serial_sequence('user_skills',          'id'), COALESCE((SELECT MAX(id) FROM user_skills),          1));
SELECT setval(pg_get_serial_sequence('jobs',                 'id'), COALESCE((SELECT MAX(id) FROM jobs),                 1));
SELECT setval(pg_get_serial_sequence('job_attachments',      'id'), COALESCE((SELECT MAX(id) FROM job_attachments),      1));
SELECT setval(pg_get_serial_sequence('proposals',            'id'), COALESCE((SELECT MAX(id) FROM proposals),            1));
SELECT setval(pg_get_serial_sequence('proposal_milestones',  'id'), COALESCE((SELECT MAX(id) FROM proposal_milestones),  1));
SELECT setval(pg_get_serial_sequence('contracts',            'id'), COALESCE((SELECT MAX(id) FROM contracts),            1));
SELECT setval(pg_get_serial_sequence('payouts',              'id'), COALESCE((SELECT MAX(id) FROM payouts),              1));
SELECT setval(pg_get_serial_sequence('promo_codes',          'id'), COALESCE((SELECT MAX(id) FROM promo_codes),          1));
SELECT setval(pg_get_serial_sequence('payout_promos',        'id'), COALESCE((SELECT MAX(id) FROM payout_promos),        1));
