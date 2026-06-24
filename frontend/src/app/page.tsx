import Link from 'next/link'
import {
  ArrowRight, Zap, Shield, BarChart3, Globe,
  CheckCircle, GitBranch, Database, Users, DollarSign, Star,
  ChevronRight, Code2, Layers, Activity,
} from 'lucide-react'

const STATS = [
  { value: '5',    label: 'Microservices' },
  { value: '6',    label: 'Database backends' },
  { value: '99.9%',label: 'Uptime SLA' },
  { value: '100%', label: 'Saga Pattern Live' },
]

const FEATURES = [
  {
    icon: GitBranch,
    title: 'Choreography Saga',
    desc: 'No central orchestrator — each service publishes domain events over RabbitMQ and reacts asynchronously. Full compensation on failure.',
    color: 'from-violet-500 to-purple-600',
    bg: 'bg-violet-50 dark:bg-violet-900/20',
    text: 'text-violet-600 dark:text-violet-400',
  },
  {
    icon: Shield,
    title: 'Escrow Payments',
    desc: 'Wallet service holds funds in escrow and releases them only when both parties confirm delivery. Automatic refund on failure.',
    color: 'from-emerald-500 to-teal-600',
    bg: 'bg-emerald-50 dark:bg-emerald-900/20',
    text: 'text-emerald-600 dark:text-emerald-400',
  },
  {
    icon: BarChart3,
    title: 'Full Observability',
    desc: 'Prometheus metrics, Grafana dashboards, ELK log aggregation, and Spring Boot Actuator health endpoints on every service.',
    color: 'from-orange-500 to-amber-600',
    bg: 'bg-orange-50 dark:bg-orange-900/20',
    text: 'text-orange-600 dark:text-orange-400',
  },
  {
    icon: Globe,
    title: 'Multi-DB Architecture',
    desc: 'PostgreSQL, MongoDB, Redis, Elasticsearch, Neo4j, and Cassandra — each chosen for its workload. Graceful degradation built in.',
    color: 'from-brand-500 to-indigo-600',
    bg: 'bg-brand-50 dark:bg-brand-900/20',
    text: 'text-brand-600 dark:text-brand-400',
  },
]

const HOW_IT_WORKS_CLIENT = [
  { step: '01', title: 'Post a Job',         desc: 'Describe your project, set a budget, and publish it to the marketplace.' },
  { step: '02', title: 'Review Proposals',   desc: 'Receive bids from verified freelancers. Accept the best fit.' },
  { step: '03', title: 'Pay on Completion',  desc: 'The saga handles the payout automatically when work is confirmed.' },
]

const HOW_IT_WORKS_FREELANCER = [
  { step: '01', title: 'Browse Open Jobs',   desc: 'Search and filter by category, budget, or skills required.' },
  { step: '02', title: 'Submit a Proposal',  desc: 'Write your cover letter and set your bid. Get shortlisted.' },
  { step: '03', title: 'Get Paid Instantly', desc: 'Mark work complete — funds are released to your wallet via the saga.' },
]

const TECH_STACK = [
  { icon: Code2,     label: 'Spring Boot 4',  desc: 'All 5 services + API Gateway' },
  { icon: Database,  label: '6 Databases',    desc: 'PG · Mongo · Redis · ES · Neo4j · Cassandra' },
  { icon: Layers,    label: 'Kubernetes',      desc: '72 YAML manifests + Helm' },
  { icon: Activity,  label: 'RabbitMQ Saga',  desc: 'topic exchange + DLQ' },
  { icon: BarChart3, label: 'Prometheus+Grafana', desc: 'Metrics + dashboards' },
  { icon: GitBranch, label: 'Next.js 14',     desc: 'Frontend with TanStack Query' },
]

const TESTIMONIALS = [
  {
    quote: 'The choreography saga pattern ensures every service stays loosely coupled — contracts are created the moment a proposal is accepted, with zero manual wiring.',
    author: 'Contract Service',
    role: 'Contract Event Consumer',
    initial: 'C',
    color: 'bg-violet-100 text-violet-700 dark:bg-violet-900/40 dark:text-violet-300',
  },
  {
    quote: 'Failed payments trigger compensation automatically. The dead-letter queue holds the message for inspection and the wallet retries or issues a refund.',
    author: 'Wallet Service',
    role: 'Payment & Refund Handler',
    initial: 'W',
    color: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
  },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white dark:bg-gray-950 overflow-x-hidden">

      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 border-b border-gray-200/80 bg-white/80 backdrop-blur-md dark:border-gray-800/80 dark:bg-gray-950/80">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white text-sm font-bold shadow-sm">F</div>
            <span className="text-lg font-bold text-gray-900 dark:text-white">Freedance</span>
          </div>
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-gray-600 dark:text-gray-400">
            <a href="#features" className="hover:text-gray-900 dark:hover:text-white transition-colors">Features</a>
            <a href="#how-it-works" className="hover:text-gray-900 dark:hover:text-white transition-colors">How it works</a>
            <a href="#tech" className="hover:text-gray-900 dark:hover:text-white transition-colors">Tech Stack</a>
          </nav>
          <div className="flex items-center gap-3">
            <Link href="/login" className="hidden sm:inline-flex items-center rounded-lg px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white transition-colors">
              Sign in
            </Link>
            <Link href="/register" className="inline-flex items-center gap-1.5 rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-brand-700 transition-colors">
              Get started <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </header>

      {/* ── Hero ───────────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden py-20 sm:py-28">
        {/* Background glows */}
        <div className="pointer-events-none absolute inset-0 -z-10">
          <div className="absolute left-1/4 top-0 h-[500px] w-[500px] -translate-x-1/2 -translate-y-1/4 rounded-full bg-brand-400/20 blur-[120px] dark:bg-brand-500/10" />
          <div className="absolute right-1/4 top-1/3 h-[400px] w-[400px] translate-x-1/2 rounded-full bg-violet-400/20 blur-[120px] dark:bg-violet-500/10" />
        </div>

        <div className="mx-auto max-w-7xl px-6 text-center">
          {/* Badge */}
          <div className="mb-8 inline-flex items-center gap-2 rounded-full border border-brand-200 bg-brand-50 px-4 py-1.5 text-sm font-medium text-brand-700 dark:border-brand-800 dark:bg-brand-900/30 dark:text-brand-300">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-500 opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-500" />
            </span>
            Event-Driven · Choreography Saga Architecture
          </div>

          {/* Headline */}
          <h1 className="mx-auto mb-6 max-w-4xl text-5xl sm:text-6xl lg:text-7xl font-extrabold leading-[1.08] tracking-tight text-gray-900 dark:text-white">
            The freelance platform
            <br />
            <span className="bg-gradient-to-r from-brand-600 via-violet-600 to-brand-500 bg-clip-text text-transparent">
              built for engineers.
            </span>
          </h1>

          <p className="mx-auto mb-10 max-w-2xl text-lg sm:text-xl text-gray-600 dark:text-gray-400 leading-relaxed">
            Freedance is a cloud-native microservices marketplace with event-driven payment sagas,
            full observability, and multi-database architecture — running on Kubernetes.
          </p>

          {/* CTAs */}
          <div className="flex flex-wrap justify-center gap-4">
            <Link href="/register" className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-7 py-3.5 text-base font-semibold text-white shadow-lg shadow-brand-600/25 hover:bg-brand-700 transition-all hover:shadow-brand-600/40 hover:scale-[1.02]">
              Start for free <ArrowRight className="h-4 w-4" />
            </Link>
            <Link href="/login" className="inline-flex items-center gap-2 rounded-xl border border-gray-300 bg-white px-7 py-3.5 text-base font-semibold text-gray-700 hover:bg-gray-50 hover:border-gray-400 transition-all dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800">
              View demo
            </Link>
          </div>

          {/* Stats row */}
          <div className="mt-16 grid grid-cols-2 gap-4 sm:grid-cols-4 max-w-3xl mx-auto">
            {STATS.map(({ value, label }) => (
              <div key={label} className="rounded-2xl border border-gray-200 bg-white/80 p-5 shadow-sm backdrop-blur dark:border-gray-800 dark:bg-gray-900/80">
                <p className="text-3xl font-black text-gray-900 dark:text-white">{value}</p>
                <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ───────────────────────────────────────────────────────── */}
      <section id="features" className="py-24 bg-gray-50/70 dark:bg-gray-900/50">
        <div className="mx-auto max-w-7xl px-6">
          <div className="text-center mb-16">
            <p className="text-sm font-semibold uppercase tracking-widest text-brand-600 dark:text-brand-400">Core Capabilities</p>
            <h2 className="mt-3 text-4xl font-bold text-gray-900 dark:text-white">
              Enterprise architecture, production ready
            </h2>
            <p className="mt-4 max-w-2xl mx-auto text-gray-600 dark:text-gray-400">
              Every feature is backed by a proper microservice with its own database, health checks, and event publishing.
            </p>
          </div>

          <div className="grid gap-6 sm:grid-cols-2">
            {FEATURES.map(({ icon: Icon, title, desc, color, bg, text }) => (
              <div key={title} className="group relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-8 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 dark:border-gray-800 dark:bg-gray-900">
                <div className={`mb-5 inline-flex rounded-xl p-3 ${bg}`}>
                  <Icon className={`h-6 w-6 ${text}`} />
                </div>
                <h3 className="mb-2 text-xl font-bold text-gray-900 dark:text-white">{title}</h3>
                <p className="text-gray-600 dark:text-gray-400 leading-relaxed">{desc}</p>
                {/* Gradient corner accent */}
                <div className={`absolute right-0 top-0 h-24 w-24 rounded-bl-[3rem] bg-gradient-to-br ${color} opacity-0 group-hover:opacity-5 transition-opacity`} />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── How it works ───────────────────────────────────────────────────── */}
      <section id="how-it-works" className="py-24">
        <div className="mx-auto max-w-7xl px-6">
          <div className="text-center mb-16">
            <p className="text-sm font-semibold uppercase tracking-widest text-brand-600 dark:text-brand-400">Simple by design</p>
            <h2 className="mt-3 text-4xl font-bold text-gray-900 dark:text-white">
              How it works
            </h2>
          </div>

          <div className="grid gap-16 lg:grid-cols-2">
            {/* For Clients */}
            <div>
              <div className="mb-8 flex items-center gap-3">
                <span className="rounded-full bg-brand-100 px-4 py-1.5 text-sm font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">For Clients</span>
                <Users className="h-5 w-5 text-brand-500" />
              </div>
              <div className="space-y-6">
                {HOW_IT_WORKS_CLIENT.map(({ step, title, desc }, i) => (
                  <div key={step} className="flex gap-5">
                    <div className="flex flex-col items-center">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-brand-200 bg-brand-50 text-sm font-black text-brand-600 dark:border-brand-800 dark:bg-brand-900/20 dark:text-brand-300">
                        {step}
                      </div>
                      {i < HOW_IT_WORKS_CLIENT.length - 1 && (
                        <div className="mt-2 flex-1 w-0.5 bg-brand-100 dark:bg-brand-900/40 min-h-[2rem]" />
                      )}
                    </div>
                    <div className="pb-6">
                      <h4 className="font-bold text-gray-900 dark:text-white">{title}</h4>
                      <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* For Freelancers */}
            <div>
              <div className="mb-8 flex items-center gap-3">
                <span className="rounded-full bg-emerald-100 px-4 py-1.5 text-sm font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300">For Freelancers</span>
                <DollarSign className="h-5 w-5 text-emerald-500" />
              </div>
              <div className="space-y-6">
                {HOW_IT_WORKS_FREELANCER.map(({ step, title, desc }, i) => (
                  <div key={step} className="flex gap-5">
                    <div className="flex flex-col items-center">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-emerald-200 bg-emerald-50 text-sm font-black text-emerald-600 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
                        {step}
                      </div>
                      {i < HOW_IT_WORKS_FREELANCER.length - 1 && (
                        <div className="mt-2 flex-1 w-0.5 bg-emerald-100 dark:bg-emerald-900/40 min-h-[2rem]" />
                      )}
                    </div>
                    <div className="pb-6">
                      <h4 className="font-bold text-gray-900 dark:text-white">{title}</h4>
                      <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Tech Stack ─────────────────────────────────────────────────────── */}
      <section id="tech" className="py-24 bg-gray-900 dark:bg-gray-950">
        <div className="mx-auto max-w-7xl px-6">
          <div className="text-center mb-14">
            <p className="text-sm font-semibold uppercase tracking-widest text-brand-400">Under the hood</p>
            <h2 className="mt-3 text-4xl font-bold text-white">
              Built on a serious stack
            </h2>
            <p className="mt-4 max-w-xl mx-auto text-gray-400">
              Six backend services, six databases, and a full Kubernetes deployment with monitoring — all wired together with event-driven saga choreography.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {TECH_STACK.map(({ icon: Icon, label, desc }) => (
              <div key={label} className="flex items-start gap-4 rounded-xl border border-gray-800 bg-gray-800/40 p-5 hover:border-gray-700 hover:bg-gray-800/60 transition-colors">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-700 text-brand-400">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold text-white">{label}</p>
                  <p className="mt-0.5 text-sm text-gray-400">{desc}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Saga flow diagram text */}
          <div className="mt-12 rounded-2xl border border-gray-800 bg-gray-950/80 p-6 overflow-x-auto">
            <p className="mb-4 text-xs font-semibold uppercase tracking-widest text-gray-500">FreeDance Choreography Event Flow</p>
            <div className="flex items-center gap-0 min-w-max text-xs font-mono">
              {[
                { label: 'proposal-service', event: 'PROPOSAL_ACCEPTED', color: 'text-brand-400' },
                { label: 'contract-service', event: 'CONTRACT_CREATED',   color: 'text-violet-400' },
                { label: 'proposal-service', event: 'PROPOSAL_COMPLETING', color: 'text-amber-400' },
                { label: 'wallet-service',   event: 'PAYMENT_COMPLETED',  color: 'text-emerald-400' },
                { label: 'contract-service', event: 'COMPLETED',          color: 'text-emerald-400' },
              ].map(({ label, event, color }, i, arr) => (
                <div key={i} className="flex items-center">
                  <div className="flex flex-col items-center gap-1">
                    <span className={`font-bold ${color}`}>{event}</span>
                    <span className="text-gray-600 text-[10px]">→ {label}</span>
                  </div>
                  {i < arr.length - 1 && (
                    <ChevronRight className="h-4 w-4 mx-3 text-gray-600 shrink-0" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── Architecture Notes ─────────────────────────────────────────────── */}
      <section className="py-24">
        <div className="mx-auto max-w-7xl px-6">
          <div className="text-center mb-14">
            <p className="text-sm font-semibold uppercase tracking-widest text-brand-600 dark:text-brand-400">Architecture insights</p>
            <h2 className="mt-3 text-4xl font-bold text-gray-900 dark:text-white">Service highlights</h2>
          </div>
          <div className="grid gap-6 md:grid-cols-2">
            {TESTIMONIALS.map(({ quote, author, role, initial, color }) => (
              <div key={author} className="relative rounded-2xl border border-gray-200 bg-white p-8 shadow-sm dark:border-gray-800 dark:bg-gray-900">
                <div className="mb-6 text-brand-400 opacity-40">
                  <svg width="40" height="28" viewBox="0 0 40 28" fill="currentColor">
                    <path d="M0 28V16.4C0 7.6 5.2 2.4 15.6 0l2.4 4C12.4 5.6 9.2 8.8 8.8 14H16V28H0zm24 0V16.4C24 7.6 29.2 2.4 39.6 0l2.4 4C36.4 5.6 33.2 8.8 32.8 14H40V28H24z" />
                  </svg>
                </div>
                <p className="text-gray-700 dark:text-gray-300 leading-relaxed">{quote}</p>
                <div className="mt-6 flex items-center gap-3">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold ${color}`}>{initial}</div>
                  <div>
                    <p className="font-semibold text-gray-900 dark:text-white">{author}</p>
                    <p className="text-sm text-gray-500">{role}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ────────────────────────────────────────────────────────────── */}
      <section className="py-20">
        <div className="mx-auto max-w-7xl px-6">
          <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 via-brand-700 to-violet-700 p-12 text-center shadow-2xl">
            {/* Background circles */}
            <div className="pointer-events-none absolute inset-0 overflow-hidden">
              <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/5" />
              <div className="absolute -bottom-16 -left-16 h-80 w-80 rounded-full bg-white/5" />
            </div>

            <div className="relative">
              <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium text-white/90">
                <CheckCircle className="h-4 w-4" /> No credit card required
              </div>
              <h2 className="mb-4 text-4xl font-extrabold text-white">
                Ready to build something great?
              </h2>
              <p className="mb-8 text-xl text-white/80">
                Join as a client to find top talent, or as a freelancer to showcase your skills.
              </p>
              <div className="flex flex-wrap justify-center gap-4">
                <Link href="/register" className="inline-flex items-center gap-2 rounded-xl bg-white px-7 py-3.5 text-base font-semibold text-brand-700 shadow-lg hover:bg-gray-50 transition-all hover:scale-[1.02]">
                  Create your account <ArrowRight className="h-4 w-4" />
                </Link>
                <Link href="/login" className="inline-flex items-center gap-2 rounded-xl border border-white/30 px-7 py-3.5 text-base font-semibold text-white hover:bg-white/10 transition-colors">
                  Sign in instead
                </Link>
              </div>

              {/* Trust badges */}
              <div className="mt-10 flex flex-wrap items-center justify-center gap-6 text-sm text-white/60">
                {['FreeDance Platform', 'Spring Boot 4 · Next.js 14', 'Kubernetes · RabbitMQ · Saga'].map((t) => (
                  <span key={t} className="flex items-center gap-2">
                    <Star className="h-3.5 w-3.5 text-white/40" /> {t}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Footer ─────────────────────────────────────────────────────────── */}
      <footer className="border-t border-gray-200 bg-gray-50 py-10 dark:border-gray-800 dark:bg-gray-900/50">
        <div className="mx-auto max-w-7xl px-6">
          <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
            <div className="flex items-center gap-2.5">
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-600 text-white text-xs font-bold">F</div>
              <span className="font-bold text-gray-900 dark:text-white">Freedance</span>
            </div>
            <div className="flex items-center gap-6 text-sm text-gray-500">
              <Link href="/login" className="hover:text-gray-900 dark:hover:text-white transition-colors">Sign in</Link>
              <Link href="/register" className="hover:text-gray-900 dark:hover:text-white transition-colors">Register</Link>
              <Link href="/dashboard/admin" className="hover:text-gray-900 dark:hover:text-white transition-colors">Dashboard</Link>
            </div>
            <p className="text-sm text-gray-400">
              © 2026 FreeDance
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}
