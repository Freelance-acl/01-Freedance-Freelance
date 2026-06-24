# Freedance Frontend

Next.js 14 dashboard for the Freedance Freelance Marketplace microservices backend.

## Quick Start

```bash
cd frontend
cp .env.local.example .env.local   # already done — mock mode is on by default
npm install
npm run dev                         # http://localhost:3000
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:30080` | API Gateway URL (K8s NodePort) |
| `NEXT_PUBLIC_MOCK_MODE` | `true` | Use mock data without a running backend |
| `NEXT_PUBLIC_GRAFANA_URL` | `http://localhost:3001` | Grafana dashboard URL |
| `NEXT_PUBLIC_PROMETHEUS_URL` | `http://localhost:9090` | Prometheus URL |
| `NEXT_PUBLIC_KIBANA_URL` | `http://localhost:5601` | Kibana URL |
| `NEXT_PUBLIC_RABBITMQ_URL` | `http://localhost:15672` | RabbitMQ Management UI URL |

## Demo Accounts (Mock Mode)

On the login page, click a shortcut to auto-fill credentials:

| Role | Email | Password |
|---|---|---|
| Admin | admin@freelance.com | any |
| Client | alice@example.com | any |
| Freelancer | bob@example.com | any |

## Page Map

| Route | Access | Description |
|---|---|---|
| `/` | Public | Landing page |
| `/login` | Public | Login with demo shortcuts |
| `/register` | Public | Register as CLIENT or FREELANCER |
| `/dashboard` | All | Redirects to role-specific dashboard |
| `/dashboard/admin` | ADMIN | Platform metrics, revenue chart, top freelancers |
| `/dashboard/client` | CLIENT | My jobs and contracts overview |
| `/dashboard/freelancer` | FREELANCER | My proposals, contracts, payout summary |
| `/jobs` | All | Job listings with search + filter + pagination |
| `/jobs/create` | CLIENT, ADMIN | Post a new job |
| `/jobs/[id]` | All | Job detail; FREELANCER can submit proposals |
| `/proposals` | All | Proposals list with status filter + pagination |
| `/proposals/[id]` | All | Proposal detail with role-based actions |
| `/contracts` | All | Contracts list with status filter + pagination |
| `/contracts/[id]` | All | Contract detail; ADMIN/CLIENT can terminate |
| `/wallet` | FREELANCER, ADMIN | Payouts, platform fees, promo codes |
| `/users` | ADMIN | User management table with deactivate |
| `/users/[id]` | ADMIN | User profile with earnings summary |
| `/saga` | All | Choreography saga timeline + architecture |
| `/observability` | ADMIN | Links to Grafana, Prometheus, Kibana, RabbitMQ |
| `/settings` | All | Update profile, view account info |

## Switching to Live Backend

1. Start the backend: `./setup.bash && ./run.bash` (from project root)
2. Update `.env.local`:
   ```
   NEXT_PUBLIC_MOCK_MODE=false
   NEXT_PUBLIC_API_URL=http://localhost:30080   # or minikube IP
   ```
3. Restart the dev server: `npm run dev`

## Scripts

```bash
npm run dev      # Start development server on port 3000
npm run build    # Production build
npm run start    # Start production server
npm run lint     # ESLint check
```

## Stack

- **Next.js 14** App Router, TypeScript strict mode
- **TanStack Query v5** for server state and caching
- **Axios** with JWT interceptors
- **Tailwind CSS** + class-variance-authority for typed component variants
- **Recharts** for analytics charts
- **react-hook-form** + **Zod** for form validation
- **sonner** for toast notifications
- **lucide-react** for icons
