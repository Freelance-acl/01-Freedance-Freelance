## Plan: Proposal ACL and Recommendation Verification

The proposal-service already contains the core mechanics for JWT protection, proposal-event observation, DTO builders/adapters, and recommendations. A cheap static check still reports a few existing diagnostics in proposal-service (mostly unused imports/variables in tests and one Redis serializer deprecation), so the remaining work is to verify each required behavior with focused tests, clean only the blocking issues that show up during validation, and then prepare branch/PR delivery if the tree is fully green.

**Steps**
1. Confirm the implemented scope against the task list in the proposal service and identify only the files that directly control the requested behaviors. Anchor this on [ProposalSecurityConfig](proposal-service/src/main/java/com/team01/freelance/proposal/config/ProposalSecurityConfig.java), [ProposalController](proposal-service/src/main/java/com/team01/freelance/proposal/controller/ProposalController.java), [ProposalService](proposal-service/src/main/java/com/team01/freelance/proposal/service/ProposalService.java), [ProposalRecommendationService](proposal-service/src/main/java/com/team01/freelance/proposal/service/ProposalRecommendationService.java), [MongoEventLogger](proposal-service/src/main/java/com/team01/freelance/proposal/observer/MongoEventLogger.java), and the adapter/DTO classes. *Depends on nothing.*
2. Run the narrowest test set that proves each requested behavior: JWT security, proposal recommendation ACL/ranking/cache, proposal observer event emission, adapter mapping, and withdrawal/milestone flows. Prefer the existing focused integration and unit tests in proposal-service before widening to any broader module or full reactor run. *Depends on step 1.*
3. If a targeted test fails, patch only the owning slice and immediately rerun the same narrow test before moving on. Keep fixes local to the implementation already in place instead of introducing new abstractions unless the failure proves the current design is incomplete. *Depends on step 2.*
4. If the tests are green and no missing coverage remains, prepare delivery with Git/GitHub workflow: create a dedicated branch for the final verified change set, open the relevant PR with `gh`, and keep the PR scope aligned to the proposal-service work only. If the verification reveals separate concerns, split them into separate branches/PRs by behavior area rather than mixing unrelated fixes. *Depends on steps 2-3.*

**Relevant files**
- proposal-service/src/main/java/com/team01/freelance/proposal/config/ProposalSecurityConfig.java — JWT security wiring for proposal-service endpoints.
- proposal-service/src/main/java/com/team01/freelance/proposal/controller/ProposalController.java — recommendation endpoint and proposal CRUD surface.
- proposal-service/src/main/java/com/team01/freelance/proposal/service/ProposalService.java — proposal write flows that emit observer events.
- proposal-service/src/main/java/com/team01/freelance/proposal/service/ProposalRecommendationService.java — ownership check, freelancer lookup, Neo4j graph scoring, PG enrichment, and cacheable recommendations.
- proposal-service/src/main/java/com/team01/freelance/proposal/observer/MongoEventLogger.java — observer-to-Mongo persistence and cache invalidation.
- proposal-service/src/main/java/com/team01/freelance/proposal/adapter/MongoDocumentAdapter.java and proposal-service/src/main/java/com/team01/freelance/proposal/adapter/Neo4jRecordAdapter.java — DTO adapters for event and recommendation outputs.
- proposal-service/src/main/java/com/team01/freelance/proposal/dto/ProposalAnalyticsDTO.java and proposal-service/src/main/java/com/team01/freelance/proposal/dto/ProposalDetailsDTO.java — builder-based DTOs for the requested S3-F6 and S3-F9 surfaces.
- proposal-service/src/test/java/com/team01/freelance/proposal/controller/Cc1JwtSecurityIntegrationTest.java — JWT protection checks.
- proposal-service/src/test/java/com/team01/freelance/proposal/controller/ProposalRecommendationsIntegrationTest.java — recommendation ACL, ranking, empty-result, and limit behavior.
- proposal-service/src/test/java/com/team01/freelance/proposal/service/ProposalServiceObserverTest.java and proposal-service/src/test/java/com/team01/freelance/proposal/observer/MongoEventLoggerTest.java — observer emission and persistence plumbing.
- proposal-service/src/test/java/com/team01/freelance/proposal/adapter/AdapterPatternTest.java — adapter contract coverage.

**Verification**
1. Run the focused proposal-service test classes that map directly to the prompt items, then review failures only if they appear in those slices.
2. Confirm the tests prove the requested outcomes: unauthorized callers get 401/403, owner/admin callers get recommendations, proposal write events are emitted, and the adapter/DTO shapes match the expected output.
3. If the tree is fully green, capture the final branch and PR identifiers created with `gh` as the delivery artifact.

**Decisions**
- The current repository state appears to already satisfy the functional requirements; the plan is therefore validation-first, not a rewrite plan.
- Keep scope limited to proposal-service unless a focused test proves a cross-service dependency issue.
- Treat branch/PR creation as the last delivery step after verification, not as a substitute for validation.

**Further Considerations**
1. If you want the delivery split by concern, I recommend two PRs: one for security/event plumbing and one for recommendations/DTO shaping. If you want a single deliverable, keep them together under one verified branch.
2. If a test surfaces a mismatch in the prompt wording versus current implementation, decide whether to align the code to the prompt or to the repository’s existing behavior before opening the PR.
