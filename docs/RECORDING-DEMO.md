# Recording the live local demo

Start the Docker backend and the frontend on port 3000. The frontend must have:

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_MOCK_MODE=false
```

Restart the frontend after changing environment variables. The header shows **Live API** in this mode. Operational lists, dashboard charts, and the workflow view load backend records. Failed API requests show an error instead of silently substituting mock data.

## Local demo accounts

`python scripts/verify-demo.py --prepare-recording` creates these accounts through the real registration endpoint. They are kept separate from verification records so you can start a clean recording:

| Role | Email | Password |
|---|---|---|
| Client | `client@recording.freedance.local` | `DemoPass123!` |
| Freelancer | `freelancer@recording.freedance.local` | `DemoPass123!` |

Use two separate browser profiles or a normal window and an incognito window so the client and freelancer can remain signed in independently.

## Suggested recording sequence

1. Sign in as the client at `http://localhost:3000/login`.
2. Post a job with a clear title such as **Video demo: Build a marketplace dashboard**, category **WEB_DEV**, and a positive budget.
3. In the freelancer window, open that job and submit a proposal with a bid, delivery days, and cover letter.
4. In the client window, open the proposal and click **Accept Proposal**.
5. Open **Live Workflow**. Select the proposal and wait for its contract to appear. The contract is created by a RabbitMQ consumer, not by the frontend.
6. In the freelancer window, open the accepted proposal and click **Mark as Complete**.
7. In the client window, open the contract. Wait for **COMPLETED** and a pending payout, then click **Process Demo Payout**.
8. In the freelancer window, open **Wallet** and show the completed payout. Reload the page to show that it persists. The proposal eventually becomes **PAID**.
9. Show **Live Workflow** with the matching proposal, contract, and payout IDs.

The views refresh every five seconds. Allow asynchronous updates to settle before moving to the next step.

## What to say accurately

- These are real HTTP requests, database records, and RabbitMQ-driven state changes in the local Docker backend.
- Payments are simulated by the backend. No bank or payment provider transfers real money.
- **Live Workflow** shows current persisted state, not a historical message log. Use RabbitMQ's management UI at `http://localhost:15672` or container logs to show messaging separately.
- The notification dropdown is not connected to an event feed and says so explicitly.
- Observability links require separately running their corresponding tools; Docker Compose does not start Grafana, Prometheus, or Kibana.

## Repeatable checks

```powershell
# Repository root: leaves clearly named verification records for inspection
python scripts/verify-demo.py

# frontend/: uses isolated headless Chrome sessions
npm.cmd run test:demo
```

The verification script and browser checks use separate `client@demo.freedance.local` and `freelancer@demo.freedance.local` accounts.

The browser checks cover registration, login, creating a job, proposal acceptance, completion, payout release, and persistence after reload. They do not delete existing data.

## Generate a captioned video

With the frontend and Docker backend running:

```powershell
# frontend/
npm.cmd run record:demo

# Repository root (requires imageio-ffmpeg)
python scripts/render-demo-video.py
```

Each run registers separate video accounts and creates fresh persisted records. It captures real UI actions, checks API responses, and exports a captioned silent MP4 under `artifacts/demo-video/`. The same folder contains chapter notes, subtitles, screenshots, and request verification without authentication tokens.
