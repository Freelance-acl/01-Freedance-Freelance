import type {
  User, Job, Proposal, Contract, Payout,
  SagaStep, AdminDashboard, TopFreelancer,
  FreelancerPayoutSummary, PlatformFeeAnalytics,
  PromoCode, ContractAnalytics,
} from './types'

// ─── Users (20 total) ────────────────────────────────────────────────────────
// Demo accounts: alice@example.com (CLIENT), bob@example.com (FREELANCER), admin@freelance.com (ADMIN)

export const mockUsers: User[] = [
  {
    id: 1, name: 'Alice Johnson', email: 'alice@example.com', role: 'CLIENT',
    bio: 'Serial entrepreneur and startup founder. I build SaaS products for the future of work and have shipped 4 products in the last 3 years. Always looking for top talent to bring ideas to life fast.',
    active: true, createdAt: '2024-01-10T09:00:00Z',
    preferences: { notifications: true, currency: 'USD' },
  },
  {
    id: 2, name: 'Bob Martinez', email: 'bob@example.com', role: 'FREELANCER',
    bio: 'Full-stack engineer with 8+ years of experience. Specialize in React, Node.js, Spring Boot, and cloud-native architecture. Delivered 20+ projects across fintech, healthcare, and e-commerce.',
    active: true, skills: ['React', 'Node.js', 'TypeScript', 'Spring Boot', 'PostgreSQL', 'Docker', 'Kubernetes', 'GraphQL'],
    rating: 4.9, createdAt: '2024-01-12T10:00:00Z',
  },
  {
    id: 3, name: 'Carol White', email: 'carol@example.com', role: 'FREELANCER',
    bio: 'Senior UI/UX designer and frontend engineer. I craft beautiful, accessible interfaces using Figma, React, and TailwindCSS. Passionate about design systems and micro-interactions.',
    active: true, skills: ['Figma', 'React', 'Tailwind', 'TypeScript', 'Framer Motion', 'Storybook', 'Accessibility'],
    rating: 4.7, createdAt: '2024-02-01T08:00:00Z',
  },
  {
    id: 4, name: 'Admin User', email: 'admin@freelance.com', role: 'ADMIN',
    bio: 'Platform administrator. Oversees all operations, compliance, and platform health.',
    active: true, createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 5, name: 'David Park', email: 'david.park@techventures.io', role: 'CLIENT',
    bio: 'CTO of TechVentures. Building the next generation of fintech infrastructure. Strong believer in microservices and event-driven architecture.',
    active: true, createdAt: '2024-01-20T14:00:00Z',
  },
  {
    id: 6, name: 'Emma Torres', email: 'emma@urbanlogistic.co', role: 'CLIENT',
    bio: 'Product lead at UrbanLogistic. We run a last-mile delivery platform across 12 cities. Always hiring engineers who understand scale.',
    active: true, createdAt: '2024-02-10T09:30:00Z',
  },
  {
    id: 7, name: 'Frank Chen', email: 'frank.chen@cloudpivot.dev', role: 'CLIENT',
    bio: 'Founder at CloudPivot. Helping enterprises migrate legacy systems to the cloud. We move fast and need developers who can keep up.',
    active: true, createdAt: '2024-02-22T11:00:00Z',
  },
  {
    id: 8, name: 'Grace Kim', email: 'grace@marketpulse.app', role: 'CLIENT',
    bio: 'CEO at MarketPulse, a social commerce analytics startup. We help brands understand their online presence and audience engagement.',
    active: true, createdAt: '2024-03-05T10:00:00Z',
  },
  {
    id: 9, name: 'Hiroshi Tanaka', email: 'hiroshi.tanaka@dev.jp', role: 'FREELANCER',
    bio: 'Backend architect with 10+ years in Java and distributed systems. Expert in Spring Boot microservices, Kafka event streaming, and high-throughput API design. Previously at Rakuten and LINE.',
    active: true, skills: ['Java', 'Spring Boot', 'Kafka', 'MySQL', 'Redis', 'Microservices', 'gRPC', 'AWS'],
    rating: 4.8, createdAt: '2024-01-18T08:00:00Z',
  },
  {
    id: 10, name: 'Isabella Rossi', email: 'isabella.rossi@mobilecraft.eu', role: 'FREELANCER',
    bio: 'Mobile engineer specializing in React Native and Swift. I\'ve shipped 12 apps to the App Store with 4.7+ average rating. Also experienced in data visualization and dashboard UIs.',
    active: true, skills: ['React Native', 'Swift', 'iOS', 'Android', 'Firebase', 'Expo', 'Redux Toolkit'],
    rating: 4.6, createdAt: '2024-02-05T12:00:00Z',
  },
  {
    id: 11, name: 'James Wilson', email: 'james@devops.cloud', role: 'FREELANCER',
    bio: 'DevOps and platform engineer. Kubernetes certified (CKA + CKAD), Terraform expert, GitHub Actions wizard. I turn chaotic deployments into smooth, automated workflows.',
    active: true, skills: ['Kubernetes', 'Terraform', 'AWS', 'GCP', 'Helm', 'ArgoCD', 'Prometheus', 'GitHub Actions'],
    rating: 4.8, createdAt: '2024-01-25T09:00:00Z',
  },
  {
    id: 12, name: 'Karim El-Amin', email: 'karim@datasci.io', role: 'FREELANCER',
    bio: 'Data scientist and ML engineer. Specialize in predictive modeling, NLP, and time-series forecasting. PhD in Computer Science. Former Kaggle Grandmaster.',
    active: true, skills: ['Python', 'TensorFlow', 'PyTorch', 'scikit-learn', 'Spark', 'Airflow', 'dbt', 'SQL'],
    rating: 4.9, createdAt: '2024-02-08T14:00:00Z',
  },
  {
    id: 13, name: 'Lena Schmidt', email: 'lena.schmidt@frontcraft.de', role: 'FREELANCER',
    bio: 'Frontend specialist focused on performance and accessibility. I build pixel-perfect UIs with React, Next.js, and Tailwind. Ex-Zalando senior engineer. WCAG 2.1 AA certified.',
    active: true, skills: ['React', 'Next.js', 'TypeScript', 'Tailwind', 'Radix UI', 'Testing Library', 'Web Performance'],
    rating: 4.7, createdAt: '2024-02-14T11:00:00Z',
  },
  {
    id: 14, name: 'Marco Oliveira', email: 'marco@blockchainlab.br', role: 'FREELANCER',
    bio: 'Blockchain engineer and smart contract auditor. Solidity, Rust, and Move developer. Have audited $400M+ in TVL. Certified by Consensys and OpenZeppelin.',
    active: true, skills: ['Solidity', 'Rust', 'Web3.js', 'Hardhat', 'Foundry', 'Ethereum', 'Polygon', 'Security Auditing'],
    rating: 4.8, createdAt: '2024-03-01T10:00:00Z',
  },
  {
    id: 15, name: 'Nina Patel', email: 'nina.patel@contentcraft.in', role: 'FREELANCER',
    bio: 'Technical writer and content strategist with 6 years of experience. I create documentation, blog posts, API guides, and growth content for SaaS companies. SEO-certified.',
    active: true, skills: ['Technical Writing', 'SEO', 'Markdown', 'API Documentation', 'Content Strategy', 'Notion', 'Copywriting'],
    rating: 4.5, createdAt: '2024-03-10T09:00:00Z',
  },
  {
    id: 16, name: 'Omar Hassan', email: 'omar.hassan@fullstack.eg', role: 'FREELANCER',
    bio: 'Full-stack developer with deep expertise in Node.js, React, and MongoDB. I\'ve built marketplace platforms, real-time apps, and REST/GraphQL APIs. Fast delivery, clean code.',
    active: true, skills: ['Node.js', 'React', 'MongoDB', 'GraphQL', 'Socket.io', 'Express', 'Next.js', 'Stripe API'],
    rating: 4.6, createdAt: '2024-02-19T08:00:00Z',
  },
  {
    id: 17, name: 'Priya Sharma', email: 'priya@aidevs.in', role: 'FREELANCER',
    bio: 'ML engineer and AI product builder. I integrate LLMs, build RAG pipelines, and deploy production-grade AI features. Previously at OpenAI and Anthropic as a contractor.',
    active: true, skills: ['Python', 'LangChain', 'OpenAI API', 'Pinecone', 'FastAPI', 'Docker', 'PyTorch', 'Prompt Engineering'],
    rating: 4.9, createdAt: '2024-01-30T16:00:00Z',
  },
  {
    id: 18, name: 'Quinn Murphy', email: 'quinn@launchpad.io', role: 'CLIENT',
    bio: 'Product manager turned founder. Building developer tooling and automation products. Love working with freelancers who ship fast and communicate clearly.',
    active: true, createdAt: '2024-03-18T10:00:00Z',
  },
  {
    id: 19, name: 'Sofia Nguyen', email: 'sofia.nguyen@qaconsult.vn', role: 'FREELANCER',
    bio: 'QA engineer and test automation specialist. Cypress, Playwright, Selenium expert. I build end-to-end test suites that give teams confidence to ship without fear.',
    active: true, skills: ['Cypress', 'Playwright', 'Selenium', 'Jest', 'Vitest', 'TestRail', 'Postman', 'k6'],
    rating: 4.5, createdAt: '2024-03-12T11:00:00Z',
  },
  {
    id: 20, name: 'Tyler Brooks', email: 'tyler@securityedge.us', role: 'FREELANCER',
    bio: 'Offensive security engineer. CEH, OSCP, and AWS Security certified. I find critical vulnerabilities before attackers do. Conducted 50+ penetration tests for fintech and healthcare clients.',
    active: true, skills: ['Penetration Testing', 'OWASP', 'Burp Suite', 'Metasploit', 'Network Security', 'AWS Security', 'Threat Modeling'],
    rating: 4.8, createdAt: '2024-02-28T09:00:00Z',
  },
]

// ─── Jobs (32 total) ─────────────────────────────────────────────────────────
// Alice (clientId:1) has 15 jobs | Others: David(5), Emma(6), Frank(7), Grace(8), Quinn(18)

export const mockJobs: Job[] = [
  // Alice's jobs (clientId: 1)
  {
    id: 101, title: 'Build a React Analytics Dashboard', clientId: 1,
    description: 'Need a production-ready analytics dashboard with dark/light mode, real-time charts using Recharts, and a modular component library. Must handle large datasets efficiently with virtualization.',
    budget: 3200, status: 'OPEN', category: 'Frontend',
    requirements: 'React 18+, TypeScript strict, TailwindCSS, Recharts, TanStack Query, vitest for tests',
    createdAt: '2024-06-01T10:00:00Z', updatedAt: '2024-06-01T10:00:00Z', rating: 4,
  },
  {
    id: 102, title: 'API Integration & Backend Services', clientId: 1,
    description: 'Integrate Stripe, Twilio, and ShipEngine APIs into our existing Spring Boot application. Must include webhook handling, retry logic, and idempotency. Full test coverage expected.',
    budget: 4500, status: 'IN_PROGRESS', category: 'Backend',
    requirements: 'Spring Boot 3+, REST, OpenAPI 3, JUnit 5, Stripe SDK, Webhooks',
    createdAt: '2024-05-15T08:00:00Z', updatedAt: '2024-06-05T12:00:00Z', rating: 3,
  },
  {
    id: 103, title: 'Mobile App UI Design (Figma)', clientId: 1,
    description: 'Design 40+ Figma screens for a cross-platform marketplace app. Includes component library, design tokens, onboarding flow, product listings, and checkout. Deliverable: Dev-ready Figma file.',
    budget: 1950, status: 'CLOSED', category: 'Design',
    requirements: 'Figma, Auto Layout, design systems, mobile-first, iOS and Android specs',
    createdAt: '2024-04-10T09:00:00Z', updatedAt: '2024-05-19T11:00:00Z', rating: 5,
  },
  {
    id: 104, title: 'DevOps & Kubernetes Cluster Setup', clientId: 1,
    description: 'Configure a production Kubernetes cluster on AWS EKS. Set up ingress controller, cert-manager, Prometheus/Grafana monitoring, and a full CI/CD pipeline using GitHub Actions and ArgoCD.',
    budget: 3900, status: 'IN_PROGRESS', category: 'DevOps',
    requirements: 'Kubernetes, Helm, GitHub Actions, ArgoCD, Prometheus, Grafana, AWS EKS',
    createdAt: '2024-06-08T14:00:00Z', updatedAt: '2024-06-10T09:00:00Z', rating: 4,
  },
  {
    id: 105, title: 'E-Commerce Platform Full Development', clientId: 1,
    description: 'Build a multi-vendor e-commerce platform with product catalog, cart, checkout, vendor dashboards, and order management. Must support 10k+ concurrent users.',
    budget: 8500, status: 'CLOSED', category: 'Backend',
    requirements: 'Spring Boot, PostgreSQL, Redis caching, Stripe, Elasticsearch, Docker Compose',
    createdAt: '2024-03-01T10:00:00Z', updatedAt: '2024-05-10T16:00:00Z', rating: 5,
  },
  {
    id: 106, title: 'Data Analytics Pipeline & Dashboards', clientId: 1,
    description: 'Build an end-to-end analytics pipeline ingesting 50M+ daily events. Design a Spark ETL, load into BigQuery, and create executive dashboards in Looker Studio.',
    budget: 6200, status: 'CLOSED', category: 'Data Science',
    requirements: 'Apache Spark, BigQuery, Python, dbt, Looker Studio, Airflow',
    createdAt: '2024-03-15T09:00:00Z', updatedAt: '2024-05-25T14:00:00Z', rating: 5,
  },
  {
    id: 107, title: 'SaaS Landing Page & Marketing Site Redesign', clientId: 1,
    description: 'Redesign our SaaS landing page from scratch. Focus on conversion optimization, animations with Framer Motion, A/B test infrastructure, and CMS integration (Sanity or Contentful).',
    budget: 2800, status: 'OPEN', category: 'Frontend',
    requirements: 'Next.js 14, TailwindCSS, Framer Motion, Sanity CMS, Vercel Analytics, GTM integration',
    createdAt: '2024-06-15T11:00:00Z', updatedAt: '2024-06-15T11:00:00Z', rating: 4,
  },
  {
    id: 108, title: 'Real-Time Chat & Collaboration App', clientId: 1,
    description: 'Build a real-time chat platform with channels, direct messages, file sharing, and typing indicators. Must scale to 50k concurrent connections using WebSockets with a Redis pub/sub backend.',
    budget: 5200, status: 'IN_PROGRESS', category: 'Backend',
    requirements: 'Node.js, Socket.io, Redis Pub/Sub, React, PostgreSQL, S3 for attachments',
    createdAt: '2024-05-20T08:00:00Z', updatedAt: '2024-06-08T10:00:00Z', rating: 4,
  },
  {
    id: 109, title: 'Automated E2E Testing Suite', clientId: 1,
    description: 'Build a comprehensive end-to-end test suite for our web application covering 200+ user flows. Must integrate with GitHub Actions CI and generate Allure reports.',
    budget: 3500, status: 'IN_PROGRESS', category: 'QA',
    requirements: 'Playwright, TypeScript, GitHub Actions, Allure Reports, Percy for visual regression',
    createdAt: '2024-05-28T14:00:00Z', updatedAt: '2024-06-05T09:00:00Z', rating: 4,
  },
  {
    id: 110, title: 'ML-Based Dynamic Pricing Engine', clientId: 1,
    description: 'Develop a machine learning system to predict optimal product pricing based on demand, seasonality, competitor data, and inventory levels. Need real-time inference API.',
    budget: 7200, status: 'IN_PROGRESS', category: 'Data Science',
    requirements: 'Python, XGBoost, FastAPI, MLflow, Docker, PostgreSQL, feature store',
    createdAt: '2024-06-03T16:00:00Z', updatedAt: '2024-06-10T11:00:00Z', rating: 4,
  },
  {
    id: 111, title: 'iOS Fitness Tracking App (Swift)', clientId: 1,
    description: 'Native iOS fitness app with workout logging, Apple HealthKit integration, animated progress rings, social challenges, and a premium subscription paywall via RevenueCat.',
    budget: 4500, status: 'CLOSED', category: 'Mobile',
    requirements: 'Swift 5.9, SwiftUI, HealthKit, StoreKit 2, RevenueCat, CoreData',
    createdAt: '2024-03-20T10:00:00Z', updatedAt: '2024-05-30T15:00:00Z', rating: 5,
  },
  {
    id: 112, title: 'Smart Contract Development & Audit', clientId: 1,
    description: 'Develop and audit ERC-20 and ERC-721 smart contracts for our NFT marketplace. Must include custom royalty logic, staking mechanism, and a governance contract. Full audit report required.',
    budget: 6800, status: 'IN_PROGRESS', category: 'Blockchain',
    requirements: 'Solidity 0.8+, Hardhat, OpenZeppelin, Foundry for testing, Slither for analysis',
    createdAt: '2024-05-10T09:00:00Z', updatedAt: '2024-06-08T13:00:00Z', rating: 4,
  },
  {
    id: 113, title: 'Headless CMS & Blog Platform', clientId: 1,
    description: 'Migrate our WordPress blog to a headless architecture using Contentful as the CMS and Next.js as the frontend. SEO must be preserved, including redirects for 500+ legacy URLs.',
    budget: 4200, status: 'CLOSED', category: 'Backend',
    requirements: 'Next.js 14, Contentful SDK, TypeScript, ISR, Sitemap generation, Redirects',
    createdAt: '2024-04-01T08:00:00Z', updatedAt: '2024-05-28T12:00:00Z', rating: 5,
  },
  {
    id: 114, title: 'Video Streaming Platform Backend', clientId: 1,
    description: 'Build the backend for a video-on-demand platform supporting HLS streaming, adaptive bitrate, subtitle tracks, and DRM. Must handle 1TB+ video storage with efficient delivery via CDN.',
    budget: 9500, status: 'OPEN', category: 'Backend',
    requirements: 'Node.js, FFmpeg, AWS MediaConvert, CloudFront, DRM (Widevine), PostgreSQL',
    createdAt: '2024-06-18T10:00:00Z', updatedAt: '2024-06-18T10:00:00Z', rating: 4,
  },
  {
    id: 115, title: 'Payment Gateway & Multi-Currency Support', clientId: 1,
    description: 'Integrate Stripe and PayPal into our checkout flow with support for 30+ currencies, automatic tax calculation (Stripe Tax), refunds, dispute handling, and a reconciliation dashboard.',
    budget: 3800, status: 'CANCELLED', category: 'Backend',
    requirements: 'Stripe, PayPal SDK, Spring Boot, multi-currency, Stripe Tax, webhook security',
    createdAt: '2024-05-05T11:00:00Z', updatedAt: '2024-05-20T10:00:00Z', rating: 3,
  },

  // David's jobs (clientId: 5)
  {
    id: 116, title: 'Mobile Banking App (React Native)', clientId: 5,
    description: 'Build a full-featured mobile banking app with biometric auth, balance dashboard, transaction history, P2P transfers, and push notifications. PCI-DSS compliant architecture required.',
    budget: 7500, status: 'CLOSED', category: 'Mobile',
    requirements: 'React Native, Expo, Plaid API, biometric auth, Stripe, push notifications (OneSignal)',
    createdAt: '2024-03-10T09:00:00Z', updatedAt: '2024-05-20T16:00:00Z', rating: 5,
  },
  {
    id: 117, title: 'Microservices Migration from Monolith', clientId: 5,
    description: 'Decompose a 200k LOC Node.js monolith into 8 microservices. Define domain boundaries, set up inter-service communication via RabbitMQ, and ensure zero-downtime migration.',
    budget: 8200, status: 'CLOSED', category: 'Backend',
    requirements: 'Node.js, RabbitMQ, Docker, Kubernetes, domain-driven design, strangler fig pattern',
    createdAt: '2024-02-15T08:00:00Z', updatedAt: '2024-05-01T12:00:00Z', rating: 5,
  },
  {
    id: 118, title: 'GraphQL API Gateway Development', clientId: 5,
    description: 'Build a GraphQL API gateway that federates 5 downstream microservices. Requires schema stitching, dataloader batching, persisted queries, and APM integration.',
    budget: 4100, status: 'OPEN', category: 'Backend',
    requirements: 'Apollo Server, GraphQL federation, TypeScript, DataLoader, Prometheus, Redis caching',
    createdAt: '2024-06-12T14:00:00Z', updatedAt: '2024-06-12T14:00:00Z', rating: 4,
  },
  {
    id: 119, title: 'Cloud Infrastructure & IaC Setup', clientId: 5,
    description: 'Design and provision cloud infrastructure on AWS using Terraform. Includes VPC, EKS cluster, RDS (multi-AZ), Elasticache, and WAF. Deliver full runbook and documentation.',
    budget: 5200, status: 'IN_PROGRESS', category: 'DevOps',
    requirements: 'Terraform, AWS, EKS, RDS, Elasticache, WAF, Terragrunt, Atlantis for GitOps',
    createdAt: '2024-06-01T11:00:00Z', updatedAt: '2024-06-14T09:00:00Z', rating: 4,
  },

  // Emma's jobs (clientId: 6)
  {
    id: 120, title: 'React Native Delivery Driver App', clientId: 6,
    description: 'Build the driver-facing app for our delivery platform. Real-time route optimization, in-app navigation, order scanning via QR code, earnings dashboard, and offline-first data sync.',
    budget: 5800, status: 'IN_PROGRESS', category: 'Mobile',
    requirements: 'React Native, Expo, Google Maps API, offline sync, barcode scanning, real-time WebSocket',
    createdAt: '2024-05-08T10:00:00Z', updatedAt: '2024-06-10T08:00:00Z', rating: 4,
  },
  {
    id: 121, title: 'AI Chatbot Customer Support Integration', clientId: 6,
    description: 'Integrate an AI chatbot for customer support using GPT-4 with a RAG pipeline. The bot should answer questions from our knowledge base and hand off to humans seamlessly.',
    budget: 3800, status: 'OPEN', category: 'Data Science',
    requirements: 'Python, LangChain, OpenAI API, Pinecone vector DB, FastAPI, Zendesk integration',
    createdAt: '2024-06-10T13:00:00Z', updatedAt: '2024-06-10T13:00:00Z', rating: 4,
  },
  {
    id: 122, title: 'WordPress to Next.js Migration', clientId: 6,
    description: 'Migrate a 1000-page WordPress site to Next.js with headless WordPress (WPGraphQL). Must preserve all SEO, page speed (target 90+ Lighthouse), and custom plugin functionality.',
    budget: 2500, status: 'OPEN', category: 'Frontend',
    requirements: 'Next.js 14, WPGraphQL, TypeScript, ISR, Yoast SEO migration, Lighthouse optimization',
    createdAt: '2024-06-14T10:00:00Z', updatedAt: '2024-06-14T10:00:00Z', rating: 4,
  },
  {
    id: 123, title: 'E-Learning Platform with Video & Quizzes', clientId: 6,
    description: 'Build an LMS-style platform with video lessons (Mux), interactive quizzes, progress tracking, certificates, and Stripe subscription billing. Mobile responsive.',
    budget: 6400, status: 'IN_PROGRESS', category: 'Frontend',
    requirements: 'Next.js, Mux video, Prisma, PostgreSQL, Stripe Billing, React Query, Radix UI',
    createdAt: '2024-05-25T09:00:00Z', updatedAt: '2024-06-12T11:00:00Z', rating: 4,
  },

  // Frank's jobs (clientId: 7)
  {
    id: 124, title: 'Penetration Testing & Security Audit', clientId: 7,
    description: 'Full penetration test of our web application, APIs, and cloud infrastructure. Deliverables: executive summary, detailed vulnerability report with CVSS scores, and remediation guidance.',
    budget: 5200, status: 'CLOSED', category: 'Security',
    requirements: 'OWASP Top 10, CVSS scoring, Burp Suite, network scanning, cloud security (AWS), report writing',
    createdAt: '2024-04-05T10:00:00Z', updatedAt: '2024-05-10T14:00:00Z', rating: 5,
  },
  {
    id: 125, title: 'Database Performance Optimization', clientId: 7,
    description: 'Our PostgreSQL database queries are taking 5-30 seconds. Need a specialist to audit queries, redesign indexes, implement read replicas, and introduce caching layers. Target: sub-200ms p99.',
    budget: 4800, status: 'IN_PROGRESS', category: 'Backend',
    requirements: 'PostgreSQL, query optimization, index design, EXPLAIN ANALYZE, PgBouncer, Redis caching',
    createdAt: '2024-06-05T09:00:00Z', updatedAt: '2024-06-14T08:00:00Z', rating: 4,
  },
  {
    id: 126, title: 'Full-Stack Web Application (Next.js + FastAPI)', clientId: 7,
    description: 'Build a B2B project management tool with kanban boards, time tracking, invoicing, team roles, and Slack integration. Next.js frontend, FastAPI backend, PostgreSQL.',
    budget: 5500, status: 'CLOSED', category: 'Backend',
    requirements: 'Next.js 14, FastAPI, PostgreSQL, SQLAlchemy, Alembic, Celery for background tasks, Docker',
    createdAt: '2024-03-25T11:00:00Z', updatedAt: '2024-05-15T13:00:00Z', rating: 5,
  },

  // Grace's jobs (clientId: 8)
  {
    id: 127, title: 'Social Media Analytics Dashboard', clientId: 8,
    description: 'Build a real-time analytics dashboard aggregating data from Instagram, TikTok, Twitter, and YouTube APIs. Include engagement rate tracking, competitor analysis, and scheduling features.',
    budget: 4500, status: 'OPEN', category: 'Frontend',
    requirements: 'React, Recharts, social media APIs, OAuth 2.0, real-time WebSocket, PostgreSQL, Celery',
    createdAt: '2024-06-16T10:00:00Z', updatedAt: '2024-06-16T10:00:00Z', rating: 4,
  },
  {
    id: 128, title: 'REST API for Social Marketplace', clientId: 8,
    description: 'Design and build a scalable REST API for a social commerce marketplace with user profiles, product listings, bidding system, real-time notifications, and payment escrow.',
    budget: 7500, status: 'CLOSED', category: 'Backend',
    requirements: 'Node.js, Express, PostgreSQL, Redis, Stripe Connect, Socket.io, Swagger docs, 90%+ test coverage',
    createdAt: '2024-03-28T08:00:00Z', updatedAt: '2024-05-22T15:00:00Z', rating: 5,
  },
  {
    id: 129, title: 'Mobile App UI/UX Redesign', clientId: 8,
    description: 'Redesign our existing mobile app UI from scratch based on user research insights. New design system, improved onboarding, simplified navigation, and dark mode support.',
    budget: 3200, status: 'IN_PROGRESS', category: 'Design',
    requirements: 'Figma, React Native (implementation), design systems, user testing, Lottie animations',
    createdAt: '2024-05-30T14:00:00Z', updatedAt: '2024-06-10T10:00:00Z', rating: 4,
  },

  // Quinn's jobs (clientId: 18)
  {
    id: 130, title: 'Chrome Extension for Productivity', clientId: 18,
    description: 'Build a Chrome extension that intercepts and summarizes web pages using the OpenAI API, tracks reading time, and syncs highlights to a personal dashboard. Manifest V3.',
    budget: 1800, status: 'OPEN', category: 'Frontend',
    requirements: 'Chrome Extension Manifest V3, TypeScript, React for options page, OpenAI API, IndexedDB',
    createdAt: '2024-06-17T09:00:00Z', updatedAt: '2024-06-17T09:00:00Z', rating: 4,
  },
  {
    id: 131, title: 'ETL Pipeline for Data Warehouse', clientId: 18,
    description: 'Build an ELT pipeline using Airbyte for ingestion, dbt for transformations, and load into Snowflake. Must include data quality checks, Slack alerts, and a lineage graph in Metabase.',
    budget: 4800, status: 'IN_PROGRESS', category: 'Data Science',
    requirements: 'Airbyte, dbt, Snowflake, Metabase, Python, Great Expectations, Prefect orchestration',
    createdAt: '2024-06-04T11:00:00Z', updatedAt: '2024-06-13T09:00:00Z', rating: 4,
  },
  {
    id: 132, title: 'Video Editing Automation with Python', clientId: 18,
    description: 'Automate batch video editing: auto-cut silence, add subtitles (Whisper), insert b-roll, encode to multiple formats, and upload to S3. Must process 100+ videos/day unattended.',
    budget: 1200, status: 'OPEN', category: 'Backend',
    requirements: 'Python, FFmpeg, Whisper API, MoviePy or PyAV, AWS S3, Lambda for serverless processing',
    createdAt: '2024-06-18T14:00:00Z', updatedAt: '2024-06-18T14:00:00Z', rating: 4,
  },
]

// ─── Proposals (60 total) ─────────────────────────────────────────────────────
// Bob (freelancerId: 2) has 15 proposals | Others make up the rest

export const mockProposals: Proposal[] = [
  // ── Bob's 15 proposals ──
  {
    id: 201, jobId: 101, freelancerId: 2, bidAmount: 3200, status: 'ACCEPTED', estimatedDays: 14,
    coverLetter: `Hi Alice — I've built 6 analytics dashboards in the last year, all in React + Recharts. My most recent one handled 2M daily data points with virtualized tables and sub-50ms renders.\n\nI'll deliver: component library (Storybook), 8 chart types, dark mode, full test coverage (vitest), and Figma handoff. I know your stack cold — TypeScript strict, TanStack Query, Tailwind. Can start Monday.`,
    createdAt: '2024-06-02T09:00:00Z', updatedAt: '2024-06-03T11:00:00Z',
  },
  {
    id: 202, jobId: 102, freelancerId: 2, bidAmount: 4500, status: 'ACCEPTED', estimatedDays: 28,
    coverLetter: `I specialize in third-party API integrations with Spring Boot. I've integrated Stripe, Twilio, and ShipEngine separately — and done exactly this combination for a logistics startup 8 months ago.\n\nMy plan: week 1 for Stripe (webhooks + idempotency), week 2 Twilio (templates + retry), week 3 ShipEngine (label generation + tracking). Full OpenAPI docs and JUnit 5 test suite included.`,
    createdAt: '2024-05-16T10:00:00Z', updatedAt: '2024-05-17T09:00:00Z',
  },
  {
    id: 203, jobId: 104, freelancerId: 2, bidAmount: 3900, status: 'COMPLETING', estimatedDays: 7,
    coverLetter: `CKA and CKAD certified — Kubernetes is what I do daily. I run 3 production EKS clusters for current clients. I can have your ingress, cert-manager, and ArgoCD GitOps pipeline live in a week.\n\nBonus: I'll include Grafana dashboards pre-built for your Spring Boot services, and document the runbook so your team can own it afterward.`,
    createdAt: '2024-06-09T08:00:00Z', updatedAt: '2024-06-14T09:00:00Z',
  },
  {
    id: 204, jobId: 108, freelancerId: 2, bidAmount: 5200, status: 'ACCEPTED', estimatedDays: 21,
    coverLetter: `I've built two real-time chat applications from scratch — one for a healthcare startup (10k concurrent users) and one for an internal enterprise tool (50k connections). Both use Socket.io + Redis pub/sub.\n\nI'll implement channels, DMs, typing indicators, read receipts, file uploads (S3 pre-signed), and message search. Full test suite with socket event testing included.`,
    createdAt: '2024-05-21T08:00:00Z', updatedAt: '2024-05-22T11:00:00Z',
  },
  {
    id: 205, jobId: 114, freelancerId: 2, bidAmount: 9200, status: 'SUBMITTED', estimatedDays: 55,
    coverLetter: `Video streaming at scale is complex — I've done it. I built a VOD platform for 200k subscribers using AWS MediaConvert, HLS, and CloudFront. Implemented Widevine DRM and adaptive bitrate from scratch.\n\nMy bid includes: transcoding pipeline, DRM integration, CDN configuration, player implementation (Video.js), and a content management API. Happy to share the architecture diagram on a call.`,
    createdAt: '2024-06-19T09:00:00Z',
  },
  {
    id: 206, jobId: 116, freelancerId: 2, bidAmount: 7500, status: 'PAID', estimatedDays: 60,
    coverLetter: `Mobile banking is a domain I take seriously — security first, UX second. I've worked with Plaid for account linking on two prior apps and implemented biometric auth (Face ID + Touch ID) with secure enclave storage.\n\nDeliverables: 30+ screens, Plaid integration, Stripe payment rails, PCI-DSS architecture review, full test suite (Jest + Detox), and App Store submission support.`,
    createdAt: '2024-03-11T10:00:00Z', updatedAt: '2024-05-21T14:00:00Z',
  },
  {
    id: 207, jobId: 117, freelancerId: 2, bidAmount: 8200, status: 'PAID', estimatedDays: 45,
    coverLetter: `Microservices migration is tricky — I've done 3 of them. The strangler fig pattern is the right approach here. I'll start by mapping domain boundaries, then extract services incrementally with zero downtime using a proxy layer.\n\nTimeline: 2 weeks for domain mapping, 4 weeks for extraction, 1 week for cutover. RabbitMQ event bus, Docker Compose for local dev, K8s manifests for prod.`,
    createdAt: '2024-02-16T09:00:00Z', updatedAt: '2024-05-02T11:00:00Z',
  },
  {
    id: 208, jobId: 118, freelancerId: 2, bidAmount: 4100, status: 'SHORTLISTED', estimatedDays: 14,
    coverLetter: `Apollo Federation is something I've implemented in production for a fintech client with 5 downstream services. The key challenges — schema ownership, @external types, dataloader batching — I've solved all of them.\n\nI'll deliver a federation-ready gateway with persisted queries, Redis query caching, and APM dashboards. Timeline: 2 weeks feels right for a 5-service federation.`,
    createdAt: '2024-06-13T09:00:00Z',
  },
  {
    id: 209, jobId: 121, freelancerId: 2, bidAmount: 3500, status: 'REJECTED', estimatedDays: 21,
    coverLetter: `I've integrated GPT-4 with RAG pipelines before and can deliver this. My vector store experience is primarily with Pinecone and pgvector. I can have a prototype in 3 days and production in 3 weeks.\n\nThe handoff-to-human flow is critical — I'll design the intent detection and escalation system carefully to minimize false positives.`,
    createdAt: '2024-06-11T14:00:00Z', updatedAt: '2024-06-12T10:00:00Z',
  },
  {
    id: 210, jobId: 122, freelancerId: 2, bidAmount: 2800, status: 'SUBMITTED', estimatedDays: 12,
    coverLetter: `I've migrated 3 WordPress sites to Next.js headless and maintained SEO throughout each time. The key is WPGraphQL + ISR + a solid redirect map generated from the WP sitemap.\n\nI'll run Lighthouse before and after each page to validate the 90+ target. Timeline: 10 business days including QA. SEO audit included free.`,
    createdAt: '2024-06-15T11:00:00Z',
  },
  {
    id: 211, jobId: 124, freelancerId: 2, bidAmount: 4800, status: 'SHORTLISTED', estimatedDays: 10,
    coverLetter: `I hold a CEH but security pen testing at this depth — full web app, API, and cloud — is really Tyler's territory. I can handle the web app and API layers but wouldn't want to oversell the cloud infrastructure piece.`,
    createdAt: '2024-04-06T09:00:00Z', updatedAt: '2024-04-08T11:00:00Z',
  },
  {
    id: 212, jobId: 126, freelancerId: 2, bidAmount: 5500, status: 'PAID', estimatedDays: 30,
    coverLetter: `Project management tools are something I've built twice before — one for a 500-person agency, one for a SaaS startup. The kanban + time tracking combo is a known pattern I can move fast on.\n\nStack: Next.js frontend, FastAPI backend, PostgreSQL with SQLAlchemy, Celery for background jobs (report generation, email digests). Slack integration via Bolt SDK. 6-week timeline, 4 weeks dev + 2 weeks QA.`,
    createdAt: '2024-03-26T10:00:00Z', updatedAt: '2024-05-16T12:00:00Z',
  },
  {
    id: 213, jobId: 129, freelancerId: 2, bidAmount: 3200, status: 'ACCEPTED', estimatedDays: 21,
    coverLetter: `UI/UX is not my strongest suit, but I've implemented many designer-to-code handoffs with Lottie animations and React Native. If the Figma is provided, I can handle implementation very efficiently.\n\nI'll build the redesign in React Native with Expo, implement the design tokens as a theme system, and ensure smooth 60fps animations using Reanimated 3.`,
    createdAt: '2024-05-31T14:00:00Z', updatedAt: '2024-06-01T10:00:00Z',
  },
  {
    id: 214, jobId: 130, freelancerId: 2, bidAmount: 1800, status: 'SUBMITTED', estimatedDays: 7,
    coverLetter: `Chrome extensions are fun to build — I've shipped two Manifest V3 extensions. The AI summarization + highlights sync is an interesting architecture challenge (background service worker + IndexedDB sync).\n\nI can deliver this in a week: MV3 manifest, content script with DOM parsing, options page in React, OpenAI streaming for summaries, and a dashboard PWA for sync.`,
    createdAt: '2024-06-18T10:00:00Z',
  },
  {
    id: 215, jobId: 131, freelancerId: 2, bidAmount: 4200, status: 'SHORTLISTED', estimatedDays: 18,
    coverLetter: `ELT pipelines are in my wheelhouse — I've set up Airbyte + dbt + Snowflake for two clients. The key is getting the dbt model layers right (staging → intermediate → mart) and the data quality tests.\n\nI'll include Great Expectations checks, Prefect DAGs, and a Metabase dashboard with auto-refresh. Slack alerting on pipeline failures included.`,
    createdAt: '2024-06-05T09:00:00Z', updatedAt: '2024-06-06T11:00:00Z',
  },

  // ── Other freelancers' proposals ──
  {
    id: 216, jobId: 101, freelancerId: 3, bidAmount: 3800, status: 'REJECTED', estimatedDays: 21,
    coverLetter: `I specialise in data visualisation and have built 4 dashboards with Recharts and D3. I can deliver a polished, animated UI with smooth transitions and mobile responsiveness. My design sensibility sets my work apart from typical developers.`,
    createdAt: '2024-06-02T14:00:00Z', updatedAt: '2024-06-03T11:00:00Z',
  },
  {
    id: 217, jobId: 101, freelancerId: 13, bidAmount: 3100, status: 'REJECTED', estimatedDays: 12,
    coverLetter: `Frontend performance is my obsession. I built a dashboard at Zalando that handled 5M data points with virtual scrolling and sub-100ms interactions. I'll make yours equally fast with full accessibility compliance.`,
    createdAt: '2024-06-02T16:00:00Z', updatedAt: '2024-06-03T11:00:00Z',
  },
  {
    id: 218, jobId: 102, freelancerId: 9, bidAmount: 4800, status: 'REJECTED', estimatedDays: 25,
    coverLetter: `I've integrated Stripe and Twilio into Spring Boot applications for 3 enterprise clients. My approach is defensive: idempotency keys, dead letter queues, circuit breakers, and comprehensive test coverage with Testcontainers.`,
    createdAt: '2024-05-16T11:00:00Z', updatedAt: '2024-05-17T09:00:00Z',
  },
  {
    id: 219, jobId: 102, freelancerId: 16, bidAmount: 4200, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `I've worked with these three APIs separately and can integrate all of them. Node.js is my main backend but I have Spring Boot experience. I'll need 5 weeks to do this properly with full test coverage.`,
    createdAt: '2024-05-17T08:00:00Z', updatedAt: '2024-05-17T09:00:00Z',
  },
  {
    id: 220, jobId: 103, freelancerId: 3, bidAmount: 1950, status: 'PAID', estimatedDays: 21,
    coverLetter: `Mobile UI design is my core expertise. I'll deliver 40+ screens in Figma with auto layout, design tokens, and a component library ready for your developers. I've designed for both iOS and Android with platform-specific guidelines. My process: discovery → wireframes → high-fidelity → developer handoff with Zeplin specs.`,
    createdAt: '2024-04-11T09:00:00Z', updatedAt: '2024-05-19T11:00:00Z',
  },
  {
    id: 221, jobId: 103, freelancerId: 10, bidAmount: 2200, status: 'REJECTED', estimatedDays: 28,
    coverLetter: `I have strong Figma skills alongside my mobile development background — I understand both the design and implementation side, which leads to practical, buildable designs.`,
    createdAt: '2024-04-11T14:00:00Z', updatedAt: '2024-04-12T10:00:00Z',
  },
  {
    id: 222, jobId: 104, freelancerId: 11, bidAmount: 4200, status: 'REJECTED', estimatedDays: 10,
    coverLetter: `Kubernetes is my daily work — CKA certified and managing 6 production clusters. I can have EKS running with ArgoCD and full observability in a week. But I think $3,900 is fair for this scope.`,
    createdAt: '2024-06-09T12:00:00Z', updatedAt: '2024-06-10T09:00:00Z',
  },
  {
    id: 223, jobId: 105, freelancerId: 9, bidAmount: 8500, status: 'PAID', estimatedDays: 45,
    coverLetter: `Multi-vendor e-commerce at scale is my domain. I've built two such platforms — one for a fashion marketplace (120k SKUs) and one for a food delivery aggregator. Elasticsearch product search, Redis sessions, Stripe Connect for vendors, and horizontal scaling with Spring Boot.`,
    createdAt: '2024-03-02T10:00:00Z', updatedAt: '2024-05-11T14:00:00Z',
  },
  {
    id: 224, jobId: 105, freelancerId: 17, bidAmount: 9200, status: 'REJECTED', estimatedDays: 60,
    coverLetter: `I can build this but it's above my typical scope. I'd want 60 days and a larger team to do it justice at the scale you described. My backend work is usually focused on AI/ML pipelines rather than e-commerce infrastructure.`,
    createdAt: '2024-03-02T15:00:00Z', updatedAt: '2024-03-03T09:00:00Z',
  },
  {
    id: 225, jobId: 106, freelancerId: 10, bidAmount: 6200, status: 'PAID', estimatedDays: 30,
    coverLetter: `I've built data pipelines handling 100M+ daily events using Spark and BigQuery. My Looker Studio dashboards are used by C-suite at two clients. I'll deliver clean, documented dbt models and interactive dashboards that your team can maintain.`,
    createdAt: '2024-03-16T10:00:00Z', updatedAt: '2024-05-26T12:00:00Z',
  },
  {
    id: 226, jobId: 106, freelancerId: 12, bidAmount: 5800, status: 'REJECTED', estimatedDays: 25,
    coverLetter: `Data pipelines are my core work but I focus more on ML model training pipelines than analytics ETL. I can do this, but Isabella is probably a better fit for the Looker dashboards specifically.`,
    createdAt: '2024-03-16T14:00:00Z', updatedAt: '2024-03-17T09:00:00Z',
  },
  {
    id: 227, jobId: 107, freelancerId: 3, bidAmount: 2800, status: 'SUBMITTED', estimatedDays: 14,
    coverLetter: `Landing page conversions are driven by design — and I've increased conversion rates by 40%+ on 3 SaaS sites I redesigned. I'll bring a portfolio of past work. Framer Motion animations are my favourite part of this kind of project.`,
    createdAt: '2024-06-16T09:00:00Z',
  },
  {
    id: 228, jobId: 107, freelancerId: 13, bidAmount: 2400, status: 'SUBMITTED', estimatedDays: 10,
    coverLetter: `Core Web Vitals and Lighthouse 100 are non-negotiable for me. I'll build this in Next.js 14 with perfect performance scores and Sanity CMS integration. I can share 3 live examples of sites I've built that score 99+ on PageSpeed.`,
    createdAt: '2024-06-16T12:00:00Z',
  },
  {
    id: 229, jobId: 108, freelancerId: 16, bidAmount: 5500, status: 'REJECTED', estimatedDays: 18,
    coverLetter: `Real-time applications are my specialty — I've built Socket.io applications for 30k concurrent users. My approach for this scope would be similar but I'd want to validate the Redis pub/sub design before committing to 50k connections.`,
    createdAt: '2024-05-21T10:00:00Z', updatedAt: '2024-05-22T11:00:00Z',
  },
  {
    id: 230, jobId: 109, freelancerId: 11, bidAmount: 3500, status: 'ACCEPTED', estimatedDays: 14,
    coverLetter: `Test automation is something I've set up from scratch 4 times now. Playwright is my preferred tool — it's faster and more reliable than Cypress at scale. I'll structure the test suite by user story, integrate with GitHub Actions, and set up Percy for visual regression. 200 flows in 2 weeks is aggressive but doable.`,
    createdAt: '2024-05-29T08:00:00Z', updatedAt: '2024-05-30T10:00:00Z',
  },
  {
    id: 231, jobId: 109, freelancerId: 19, bidAmount: 3200, status: 'SHORTLISTED', estimatedDays: 21,
    coverLetter: `QA automation is my entire career. I've written 500+ Playwright tests across multiple projects and built TestRail integration for reporting. I'd need 3 weeks but the coverage would be comprehensive and maintainable.`,
    createdAt: '2024-05-29T14:00:00Z', updatedAt: '2024-05-30T10:00:00Z',
  },
  {
    id: 232, jobId: 110, freelancerId: 12, bidAmount: 7200, status: 'ACCEPTED', estimatedDays: 40,
    coverLetter: `Dynamic pricing is my PhD research topic. I've implemented XGBoost and LightGBM pricing models in production for 2 retail clients. The key is feature engineering — I'll build a feature store with demand signals, seasonality encodings, and competitor price feeds. FastAPI for the real-time inference endpoint, MLflow for experiment tracking.`,
    createdAt: '2024-06-04T09:00:00Z', updatedAt: '2024-06-05T10:00:00Z',
  },
  {
    id: 233, jobId: 110, freelancerId: 17, bidAmount: 7800, status: 'REJECTED', estimatedDays: 50,
    coverLetter: `I usually work on LLM-based systems rather than classic ML pricing models. I could do this but Karim's background in pricing models specifically makes him a stronger candidate here.`,
    createdAt: '2024-06-04T12:00:00Z', updatedAt: '2024-06-05T10:00:00Z',
  },
  {
    id: 234, jobId: 111, freelancerId: 13, bidAmount: 4500, status: 'PAID', estimatedDays: 30,
    coverLetter: `Native iOS fitness apps are something I've shipped twice. HealthKit integration is nuanced — especially workout types, heart rate zones, and background sync. I'll implement the full app with SwiftUI, CoreData for offline-first, RevenueCat for subscriptions, and social challenge mechanics. TestFlight build within 3 weeks.`,
    createdAt: '2024-03-21T09:00:00Z', updatedAt: '2024-05-31T14:00:00Z',
  },
  {
    id: 235, jobId: 111, freelancerId: 10, bidAmount: 4800, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `I have React Native experience for fitness apps but limited Swift/SwiftUI experience for native iOS. The HealthKit integration depth you're asking for really requires a native developer.`,
    createdAt: '2024-03-21T14:00:00Z', updatedAt: '2024-03-22T10:00:00Z',
  },
  {
    id: 236, jobId: 112, freelancerId: 14, bidAmount: 6800, status: 'ACCEPTED', estimatedDays: 21,
    coverLetter: `Smart contract development and auditing is my entire career. I've audited $400M+ in TVL using Slither, Mythril, and manual code review. For your ERC-20/ERC-721 contracts, I'll also develop the staking and governance contracts with OpenZeppelin standards throughout.\n\nDeliverables: full test suite in Foundry, Slither + Mythril reports, manual audit report with severity ratings, and deployment scripts for mainnet/testnet.`,
    createdAt: '2024-05-11T09:00:00Z', updatedAt: '2024-05-12T10:00:00Z',
  },
  {
    id: 237, jobId: 112, freelancerId: 16, bidAmount: 7200, status: 'REJECTED', estimatedDays: 28,
    coverLetter: `I've written Solidity contracts but I'm not an auditor — security auditing requires a different skill set. I'd recommend Marco for the audit portion. I could assist on the NFT implementation side.`,
    createdAt: '2024-05-11T12:00:00Z', updatedAt: '2024-05-12T10:00:00Z',
  },
  {
    id: 238, jobId: 113, freelancerId: 17, bidAmount: 4200, status: 'PAID', estimatedDays: 28,
    coverLetter: `I've done 2 Contentful + Next.js migrations from WordPress. The SEO preservation piece is critical — I'll generate a full redirect map from WP's URL structure, validate it with Screaming Frog, and test each route. ISR with on-demand revalidation means your editors get instant publishing without sacrificing performance.`,
    createdAt: '2024-04-02T09:00:00Z', updatedAt: '2024-05-29T11:00:00Z',
  },
  {
    id: 239, jobId: 113, freelancerId: 15, bidAmount: 3800, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `I have experience with Contentful as a content strategist but not as a developer. I can help with content migration and structure planning but not the Next.js implementation.`,
    createdAt: '2024-04-02T14:00:00Z', updatedAt: '2024-04-03T10:00:00Z',
  },
  {
    id: 240, jobId: 114, freelancerId: 9, bidAmount: 9500, status: 'SUBMITTED', estimatedDays: 60,
    coverLetter: `Video streaming backends are complex — I've built one for a media company serving 500k subscribers. AWS MediaConvert, HLS adaptive bitrate, CloudFront with signed cookies for DRM — I know the architecture well. Will need to understand your content protection requirements (Widevine vs PlayReady vs FairPlay) before finalising the design.`,
    createdAt: '2024-06-19T10:00:00Z',
  },
  {
    id: 241, jobId: 114, freelancerId: 16, bidAmount: 8800, status: 'SUBMITTED', estimatedDays: 50,
    coverLetter: `I've worked with FFmpeg and Node.js for video processing but not at production VOD scale. I can handle the API and storage layer but would need to partner with someone experienced in MediaConvert and DRM for the streaming delivery side.`,
    createdAt: '2024-06-19T12:00:00Z',
  },
  {
    id: 242, jobId: 116, freelancerId: 9, bidAmount: 7800, status: 'REJECTED', estimatedDays: 65,
    coverLetter: `Mobile banking in React Native is outside my primary Java/Spring Boot focus. I can handle the backend APIs but the React Native frontend and PCI-DSS compliance considerations on mobile would need a specialist.`,
    createdAt: '2024-03-11T12:00:00Z', updatedAt: '2024-03-12T09:00:00Z',
  },
  {
    id: 243, jobId: 117, freelancerId: 3, bidAmount: 8500, status: 'REJECTED', estimatedDays: 50,
    coverLetter: `Microservices architecture design is engineering territory — I can contribute to the frontend API consumption and component design but the backend decomposition strategy requires a backend architect.`,
    createdAt: '2024-02-16T12:00:00Z', updatedAt: '2024-02-17T09:00:00Z',
  },
  {
    id: 244, jobId: 118, freelancerId: 11, bidAmount: 4300, status: 'SUBMITTED', estimatedDays: 14,
    coverLetter: `I've worked with Apollo Server and implemented schema stitching before (pre-Federation). Federation v2 is newer to me but I've been studying it. Happy to do a technical interview to discuss my approach before you commit.`,
    createdAt: '2024-06-13T12:00:00Z',
  },
  {
    id: 245, jobId: 119, freelancerId: 11, bidAmount: 5200, status: 'ACCEPTED', estimatedDays: 21,
    coverLetter: `Terraform on AWS is my primary day-to-day work. I'll deliver the full IaC with Terragrunt for environment management, Atlantis for GitOps plan/apply, and a complete runbook with architectural diagrams. VPC, EKS, RDS multi-AZ, Elasticache Redis cluster, and WAF rules — all in 3 weeks.`,
    createdAt: '2024-06-02T09:00:00Z', updatedAt: '2024-06-03T10:00:00Z',
  },
  {
    id: 246, jobId: 120, freelancerId: 10, bidAmount: 5800, status: 'ACCEPTED', estimatedDays: 30,
    coverLetter: `Delivery driver apps are apps I've worked on specifically — I built one for a food delivery startup. Real-time route updates, order batching, QR scanning with Expo, offline-first data sync with conflict resolution. I'll have your drivers onboarded within 30 days.`,
    createdAt: '2024-05-09T10:00:00Z', updatedAt: '2024-05-10T09:00:00Z',
  },
  {
    id: 247, jobId: 120, freelancerId: 3, bidAmount: 6200, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `Driver apps are primarily implementation rather than design work. My React Native skills are good but the offline-sync and real-time navigation aspects are beyond what I typically work on.`,
    createdAt: '2024-05-09T14:00:00Z', updatedAt: '2024-05-10T09:00:00Z',
  },
  {
    id: 248, jobId: 121, freelancerId: 12, bidAmount: 3800, status: 'SUBMITTED', estimatedDays: 28,
    coverLetter: `RAG pipelines and LLM integration are areas I've been moving into from my data science background. I've built two RAG systems with Pinecone and LangChain. The Zendesk handoff integration is the trickiest part — I'd want a discovery call to design that properly.`,
    createdAt: '2024-06-11T10:00:00Z',
  },
  {
    id: 249, jobId: 121, freelancerId: 17, bidAmount: 3400, status: 'SUBMITTED', estimatedDays: 14,
    coverLetter: `AI chatbots with RAG are literally what I do every day. I've built 5 customer support chatbots with GPT-4, LangChain, and vector databases. The intent detection and human handoff design is something I've refined over multiple projects. Can start immediately.`,
    createdAt: '2024-06-11T14:00:00Z',
  },
  {
    id: 250, jobId: 122, freelancerId: 13, bidAmount: 2500, status: 'SUBMITTED', estimatedDays: 14,
    coverLetter: `Headless WordPress + Next.js is a setup I've built 3 times. WPGraphQL is the right choice. I'll handle SEO preservation with a comprehensive redirect strategy and validate Core Web Vitals before launch. Should hit 95+ Lighthouse consistently.`,
    createdAt: '2024-06-15T10:00:00Z',
  },
  {
    id: 251, jobId: 123, freelancerId: 17, bidAmount: 6400, status: 'ACCEPTED', estimatedDays: 35,
    coverLetter: `LMS platforms at this scope — video, quizzes, certificates, Stripe billing — are something I've built once before. The Mux integration for video hosting is excellent for this use case. I'll deliver a clean, scalable architecture with Prisma and PostgreSQL. Stripe Billing with seats-based pricing included.`,
    createdAt: '2024-05-26T10:00:00Z', updatedAt: '2024-05-27T09:00:00Z',
  },
  {
    id: 252, jobId: 123, freelancerId: 16, bidAmount: 6800, status: 'REJECTED', estimatedDays: 42,
    coverLetter: `Full-stack LMS builds are complex. I can do the React/Node parts but Mux video integration and Stripe Billing subscription management are newer to me. I'd need more time than the project scope allows.`,
    createdAt: '2024-05-26T14:00:00Z', updatedAt: '2024-05-27T09:00:00Z',
  },
  {
    id: 253, jobId: 124, freelancerId: 20, bidAmount: 5200, status: 'PAID', estimatedDays: 10,
    coverLetter: `Full penetration tests are my bread and butter — OSCP certified, 50+ pen tests across fintech and healthcare. I'll cover OWASP Top 10 for the web app, BOLA/BFLA for the APIs, and the full AWS attack surface (IAM, S3, EC2 metadata). Deliverable: executive summary + detailed CVSS-scored findings + remediation roadmap.`,
    createdAt: '2024-04-06T11:00:00Z', updatedAt: '2024-05-11T13:00:00Z',
  },
  {
    id: 254, jobId: 125, freelancerId: 9, bidAmount: 4800, status: 'ACCEPTED', estimatedDays: 14,
    coverLetter: `PostgreSQL performance tuning is something I've done many times. 5-30 second queries are usually index misses or N+1 issues. My process: EXPLAIN ANALYZE every slow query, redesign indexes with partial/covering indexes where applicable, introduce PgBouncer for connection pooling, and Redis for hot data caching. Target: sub-200ms p99 is achievable.`,
    createdAt: '2024-06-06T09:00:00Z', updatedAt: '2024-06-07T10:00:00Z',
  },
  {
    id: 255, jobId: 126, freelancerId: 16, bidAmount: 5800, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `Full-stack project management tools are in my wheelhouse with Node.js. FastAPI is newer for me but doable. The Celery background tasks and Slack Bolt integration I've done before. Interested if Bob isn't available.`,
    createdAt: '2024-03-26T14:00:00Z', updatedAt: '2024-03-27T09:00:00Z',
  },
  {
    id: 256, jobId: 127, freelancerId: 12, bidAmount: 4500, status: 'SUBMITTED', estimatedDays: 21,
    coverLetter: `Social media analytics dashboards require strong data engineering alongside frontend skills. I can handle the API ingestion pipelines (Instagram Graph, TikTok Business, YouTube Data v3), data modeling, and connect to a BI layer. The React dashboard part I'd want to collaborate on with a frontend specialist.`,
    createdAt: '2024-06-17T10:00:00Z',
  },
  {
    id: 257, jobId: 128, freelancerId: 16, bidAmount: 7500, status: 'PAID', estimatedDays: 30,
    coverLetter: `Marketplace REST APIs are my core expertise — I've built 4 of them with Node.js. The escrow/payment flow with Stripe Connect is something I've implemented specifically for a freelance marketplace (similar to this platform!). Real-time notifications with Socket.io, 90%+ Jest test coverage, and full Swagger documentation included.`,
    createdAt: '2024-03-29T09:00:00Z', updatedAt: '2024-05-23T14:00:00Z',
  },
  {
    id: 258, jobId: 128, freelancerId: 9, bidAmount: 8000, status: 'REJECTED', estimatedDays: 35,
    coverLetter: `REST APIs at scale are my expertise but this is Node.js territory rather than my Java/Spring Boot stack. I'd rather pass here than deliver mediocre Node.js work.`,
    createdAt: '2024-03-29T12:00:00Z', updatedAt: '2024-03-30T09:00:00Z',
  },
  {
    id: 259, jobId: 129, freelancerId: 3, bidAmount: 3400, status: 'SUBMITTED', estimatedDays: 14,
    coverLetter: `Mobile app redesigns are where my design + frontend combination shines. I'll create the Figma system first, then implement in React Native. Dark mode, design tokens, Lottie animations — I love this kind of project. Can share 3 redesign case studies.`,
    createdAt: '2024-05-31T10:00:00Z',
  },
  {
    id: 260, jobId: 130, freelancerId: 16, bidAmount: 1900, status: 'SUBMITTED', estimatedDays: 7,
    coverLetter: `Chrome extensions are fun side projects for me — I've shipped two MV3 extensions. The OpenAI streaming for summarization is something I've done in a different context. Can deliver in a week.`,
    createdAt: '2024-06-18T09:00:00Z',
  },
  {
    id: 261, jobId: 131, freelancerId: 12, bidAmount: 4800, status: 'ACCEPTED', estimatedDays: 21,
    coverLetter: `dbt + Snowflake is my go-to stack for analytics engineering. I've built 3 end-to-end ELT pipelines with Airbyte as the source connector. My dbt models follow the staging/intermediate/mart pattern with full lineage documentation. Great Expectations for data quality and Prefect for orchestration — standard toolkit for me.`,
    createdAt: '2024-06-05T10:00:00Z', updatedAt: '2024-06-06T09:00:00Z',
  },
  {
    id: 262, jobId: 132, freelancerId: 15, bidAmount: 1200, status: 'SUBMITTED', estimatedDays: 5,
    coverLetter: `Python automation scripts for media workflows are something I've done for content agencies. FFmpeg + Whisper for auto-subtitles is a known combo. The batch processing architecture with S3 upload I can design cleanly. This is a nice focused project.`,
    createdAt: '2024-06-19T09:00:00Z',
  },
]

// ─── Contracts (28 total) ─────────────────────────────────────────────────────
// Alice (clientId:1): C301–C308, C315–C322 = 12 contracts
// Bob (freelancerId:2): C301–C308 = 8 contracts

export const mockContracts: Contract[] = [
  // Alice + Bob (active work)
  {
    id: 301, jobId: 101, proposalId: 201, freelancerId: 2, clientId: 1,
    agreedAmount: 3200, status: 'ACTIVE',
    startDate: '2024-06-04T00:00:00Z', endDate: null,
    createdAt: '2024-06-03T12:00:00Z', metadata: { priority: 'high', sprint: 1 },
  },
  {
    id: 302, jobId: 102, proposalId: 202, freelancerId: 2, clientId: 1,
    agreedAmount: 4500, status: 'ACTIVE',
    startDate: '2024-05-17T00:00:00Z', endDate: null,
    createdAt: '2024-05-17T09:00:00Z', metadata: { phase: 'integration', milestone: 'Stripe done' },
  },
  {
    id: 303, jobId: 104, proposalId: 203, freelancerId: 2, clientId: 1,
    agreedAmount: 3900, status: 'ACTIVE',
    startDate: '2024-06-10T00:00:00Z', endDate: null,
    createdAt: '2024-06-10T08:00:00Z', metadata: { env: 'production', note: 'ArgoCD deployed, payout pending' },
  },
  {
    id: 304, jobId: 108, proposalId: 204, freelancerId: 2, clientId: 1,
    agreedAmount: 5200, status: 'ACTIVE',
    startDate: '2024-05-23T00:00:00Z', endDate: null,
    createdAt: '2024-05-23T09:00:00Z', metadata: { connections_target: 50000 },
  },

  // Bob + David (completed)
  {
    id: 305, jobId: 116, proposalId: 206, freelancerId: 2, clientId: 5,
    agreedAmount: 7500, status: 'COMPLETED',
    startDate: '2024-03-12T00:00:00Z', endDate: '2024-05-21T00:00:00Z',
    createdAt: '2024-03-12T09:00:00Z', metadata: { stores: 2, app_rating: 4.8 },
  },
  {
    id: 306, jobId: 117, proposalId: 207, freelancerId: 2, clientId: 5,
    agreedAmount: 8200, status: 'COMPLETED',
    startDate: '2024-02-18T00:00:00Z', endDate: '2024-05-03T00:00:00Z',
    createdAt: '2024-02-18T09:00:00Z', metadata: { services_extracted: 8, downtime_minutes: 0 },
  },

  // Bob + Frank (completed)
  {
    id: 307, jobId: 126, proposalId: 212, freelancerId: 2, clientId: 7,
    agreedAmount: 5500, status: 'COMPLETED',
    startDate: '2024-03-28T00:00:00Z', endDate: '2024-05-16T00:00:00Z',
    createdAt: '2024-03-28T09:00:00Z', metadata: { features: 'kanban, time-tracking, invoicing, Slack', review: '5 stars' },
  },

  // Bob + Grace (active)
  {
    id: 308, jobId: 129, proposalId: 213, freelancerId: 2, clientId: 8,
    agreedAmount: 3200, status: 'ACTIVE',
    startDate: '2024-06-02T00:00:00Z', endDate: null,
    createdAt: '2024-06-02T09:00:00Z', metadata: { design_approved: true },
  },

  // Alice + Carol (completed)
  {
    id: 309, jobId: 103, proposalId: 220, freelancerId: 3, clientId: 1,
    agreedAmount: 1950, status: 'COMPLETED',
    startDate: '2024-04-12T00:00:00Z', endDate: '2024-05-19T00:00:00Z',
    createdAt: '2024-04-12T08:00:00Z', metadata: { screens: 42, components: 87 },
  },

  // Alice + Hiroshi (completed)
  {
    id: 310, jobId: 105, proposalId: 223, freelancerId: 9, clientId: 1,
    agreedAmount: 8500, status: 'COMPLETED',
    startDate: '2024-03-04T00:00:00Z', endDate: '2024-05-12T00:00:00Z',
    createdAt: '2024-03-04T09:00:00Z', metadata: { vendors: 34, peak_rps: 8200 },
  },

  // Alice + Isabella (completed)
  {
    id: 311, jobId: 106, proposalId: 225, freelancerId: 10, clientId: 1,
    agreedAmount: 6200, status: 'COMPLETED',
    startDate: '2024-03-18T00:00:00Z', endDate: '2024-05-28T00:00:00Z',
    createdAt: '2024-03-18T08:00:00Z', metadata: { daily_events: '52M', dashboards: 8 },
  },

  // Alice + James (active)
  {
    id: 312, jobId: 109, proposalId: 230, freelancerId: 11, clientId: 1,
    agreedAmount: 3500, status: 'ACTIVE',
    startDate: '2024-05-30T00:00:00Z', endDate: null,
    createdAt: '2024-05-30T09:00:00Z', metadata: { tests_written: 143, target: 200 },
  },

  // Alice + Karim (active)
  {
    id: 313, jobId: 110, proposalId: 232, freelancerId: 12, clientId: 1,
    agreedAmount: 7200, status: 'ACTIVE',
    startDate: '2024-06-06T00:00:00Z', endDate: null,
    createdAt: '2024-06-06T09:00:00Z', metadata: { model: 'XGBoost + LightGBM', mae_target: 0.08 },
  },

  // Alice + Lena (completed)
  {
    id: 314, jobId: 111, proposalId: 234, freelancerId: 13, clientId: 1,
    agreedAmount: 4500, status: 'COMPLETED',
    startDate: '2024-03-22T00:00:00Z', endDate: '2024-05-31T00:00:00Z',
    createdAt: '2024-03-22T08:00:00Z', metadata: { app_store_rating: 4.9, downloads_first_week: 2100 },
  },

  // Alice + Marco (active)
  {
    id: 315, jobId: 112, proposalId: 236, freelancerId: 14, clientId: 1,
    agreedAmount: 6800, status: 'ACTIVE',
    startDate: '2024-05-13T00:00:00Z', endDate: null,
    createdAt: '2024-05-13T09:00:00Z', metadata: { contracts_audited: 3, critical_findings: 0 },
  },

  // Alice + Priya (completed)
  {
    id: 316, jobId: 113, proposalId: 238, freelancerId: 17, clientId: 1,
    agreedAmount: 4200, status: 'COMPLETED',
    startDate: '2024-04-04T00:00:00Z', endDate: '2024-05-30T00:00:00Z',
    createdAt: '2024-04-04T08:00:00Z', metadata: { redirects: 512, lighthouse_score: 98 },
  },

  // David + James (active)
  {
    id: 317, jobId: 119, proposalId: 245, freelancerId: 11, clientId: 5,
    agreedAmount: 5200, status: 'ACTIVE',
    startDate: '2024-06-04T00:00:00Z', endDate: null,
    createdAt: '2024-06-04T09:00:00Z', metadata: { provider: 'AWS', regions: ['us-east-1', 'eu-west-1'] },
  },

  // Emma + Isabella (active)
  {
    id: 318, jobId: 120, proposalId: 246, freelancerId: 10, clientId: 6,
    agreedAmount: 5800, status: 'ACTIVE',
    startDate: '2024-05-11T00:00:00Z', endDate: null,
    createdAt: '2024-05-11T09:00:00Z', metadata: { cities: 12, beta_drivers: 340 },
  },

  // Frank + Tyler (completed)
  {
    id: 319, jobId: 124, proposalId: 253, freelancerId: 20, clientId: 7,
    agreedAmount: 5200, status: 'COMPLETED',
    startDate: '2024-04-08T00:00:00Z', endDate: '2024-05-12T00:00:00Z',
    createdAt: '2024-04-08T09:00:00Z', metadata: { critical_vulns: 2, high_vulns: 7, remediated: true },
  },

  // Frank + Hiroshi (active)
  {
    id: 320, jobId: 125, proposalId: 254, freelancerId: 9, clientId: 7,
    agreedAmount: 4800, status: 'ACTIVE',
    startDate: '2024-06-08T00:00:00Z', endDate: null,
    createdAt: '2024-06-08T09:00:00Z', metadata: { slow_queries_found: 23, optimized: 14, target_p99_ms: 200 },
  },

  // Grace + Omar (completed)
  {
    id: 321, jobId: 128, proposalId: 257, freelancerId: 16, clientId: 8,
    agreedAmount: 7500, status: 'COMPLETED',
    startDate: '2024-04-01T00:00:00Z', endDate: '2024-05-24T00:00:00Z',
    createdAt: '2024-04-01T09:00:00Z', metadata: { endpoints: 48, coverage: '94%' },
  },

  // Quinn + Karim (active)
  {
    id: 322, jobId: 131, proposalId: 261, freelancerId: 12, clientId: 18,
    agreedAmount: 4800, status: 'ACTIVE',
    startDate: '2024-06-07T00:00:00Z', endDate: null,
    createdAt: '2024-06-07T09:00:00Z', metadata: { sources: 6, models: 14, schedule: 'hourly' },
  },

  // Emma + Priya (active)
  {
    id: 323, jobId: 123, proposalId: 251, freelancerId: 17, clientId: 6,
    agreedAmount: 6400, status: 'ACTIVE',
    startDate: '2024-05-28T00:00:00Z', endDate: null,
    createdAt: '2024-05-28T09:00:00Z', metadata: { courses: 12, videos_uploaded: 47, subscribers: 380 },
  },

  // Grace + Carol (active)
  {
    id: 324, jobId: 129, proposalId: 259, freelancerId: 3, clientId: 8,
    agreedAmount: 3400, status: 'TERMINATED',
    startDate: '2024-05-01T00:00:00Z', endDate: '2024-05-22T00:00:00Z',
    createdAt: '2024-05-01T09:00:00Z', metadata: { reason: 'scope changed — client pivoted to native development' },
  },

  // Quinn + Tyler (terminated)
  {
    id: 325, jobId: 130, proposalId: 214, freelancerId: 20, clientId: 18,
    agreedAmount: 1800, status: 'COMPLETED',
    startDate: '2024-05-20T00:00:00Z', endDate: '2024-06-01T00:00:00Z',
    createdAt: '2024-05-20T09:00:00Z', metadata: { extension_users: 420, chrome_store_rating: 4.6 },
  },

  // Older completed contracts (enrich history)
  {
    id: 326, jobId: 105, proposalId: 224, freelancerId: 3, clientId: 5,
    agreedAmount: 4200, status: 'COMPLETED',
    startDate: '2024-01-15T00:00:00Z', endDate: '2024-02-28T00:00:00Z',
    createdAt: '2024-01-15T09:00:00Z', metadata: { project: 'Brand identity & design system' },
  },
  {
    id: 327, jobId: 128, proposalId: 258, freelancerId: 9, clientId: 7,
    agreedAmount: 6400, status: 'COMPLETED',
    startDate: '2024-01-08T00:00:00Z', endDate: '2024-03-01T00:00:00Z',
    createdAt: '2024-01-08T09:00:00Z', metadata: { project: 'Backend API v2 rebuild' },
  },
  {
    id: 328, jobId: 132, proposalId: 262, freelancerId: 19, clientId: 18,
    agreedAmount: 2500, status: 'COMPLETED',
    startDate: '2024-02-01T00:00:00Z', endDate: '2024-03-10T00:00:00Z',
    createdAt: '2024-02-01T09:00:00Z', metadata: { project: 'QA automation suite v1' },
  },
]

// ─── Payouts (22 total) ───────────────────────────────────────────────────────

export const mockPayouts: Payout[] = [
  // Bob's completed payouts (from C305, C306, C307)
  {
    id: 401, contractId: 305, freelancerId: 2, amount: 6750, platformFee: 750,
    status: 'COMPLETED', createdAt: '2024-05-22T10:00:00Z', processedAt: '2024-05-22T14:00:00Z',
    transactionDetails: { txId: 'tx_3Nd8Af2eZvKYlo2C0m8Dq1', method: 'ACH' },
  },
  {
    id: 402, contractId: 306, freelancerId: 2, amount: 7380, platformFee: 820,
    status: 'COMPLETED', createdAt: '2024-05-04T11:00:00Z', processedAt: '2024-05-04T16:00:00Z',
    transactionDetails: { txId: 'tx_1NcRGP2eZvKYlo2C4Bq7Kx', method: 'ACH' },
  },
  {
    id: 403, contractId: 307, freelancerId: 2, amount: 4950, platformFee: 550,
    status: 'COMPLETED', createdAt: '2024-05-17T09:00:00Z', processedAt: '2024-05-17T13:00:00Z',
    transactionDetails: { txId: 'tx_2MbQFN1eYuJXkn1B3Ap6Jw', method: 'ACH' },
  },
  // Bob's pending payouts (active contracts)
  {
    id: 404, contractId: 301, freelancerId: 2, amount: 2880, platformFee: 320,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  {
    id: 405, contractId: 302, freelancerId: 2, amount: 4050, platformFee: 450,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  {
    id: 406, contractId: 303, freelancerId: 2, amount: 3510, platformFee: 390,
    status: 'PENDING', createdAt: '2024-06-14T09:00:00Z',
  },
  {
    id: 407, contractId: 304, freelancerId: 2, amount: 4680, platformFee: 520,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  // Other freelancers' completed payouts
  {
    id: 408, contractId: 309, freelancerId: 3, amount: 1755, platformFee: 195,
    status: 'COMPLETED', createdAt: '2024-05-20T12:00:00Z', processedAt: '2024-05-21T08:00:00Z',
  },
  {
    id: 409, contractId: 310, freelancerId: 9, amount: 7650, platformFee: 850,
    status: 'COMPLETED', createdAt: '2024-05-13T10:00:00Z', processedAt: '2024-05-14T09:00:00Z',
  },
  {
    id: 410, contractId: 311, freelancerId: 10, amount: 5580, platformFee: 620,
    status: 'COMPLETED', createdAt: '2024-05-29T11:00:00Z', processedAt: '2024-05-30T08:00:00Z',
  },
  {
    id: 411, contractId: 314, freelancerId: 13, amount: 4050, platformFee: 450,
    status: 'COMPLETED', createdAt: '2024-06-01T10:00:00Z', processedAt: '2024-06-02T08:00:00Z',
  },
  {
    id: 412, contractId: 316, freelancerId: 17, amount: 3780, platformFee: 420,
    status: 'COMPLETED', createdAt: '2024-05-31T09:00:00Z', processedAt: '2024-05-31T14:00:00Z',
  },
  {
    id: 413, contractId: 319, freelancerId: 20, amount: 4680, platformFee: 520,
    status: 'COMPLETED', createdAt: '2024-05-13T10:00:00Z', processedAt: '2024-05-13T15:00:00Z',
  },
  {
    id: 414, contractId: 321, freelancerId: 16, amount: 6750, platformFee: 750,
    status: 'COMPLETED', createdAt: '2024-05-25T10:00:00Z', processedAt: '2024-05-25T16:00:00Z',
  },
  {
    id: 415, contractId: 326, freelancerId: 3, amount: 3780, platformFee: 420,
    status: 'COMPLETED', createdAt: '2024-03-01T10:00:00Z', processedAt: '2024-03-02T09:00:00Z',
  },
  {
    id: 416, contractId: 327, freelancerId: 9, amount: 5760, platformFee: 640,
    status: 'COMPLETED', createdAt: '2024-03-03T11:00:00Z', processedAt: '2024-03-04T08:00:00Z',
  },
  {
    id: 417, contractId: 328, freelancerId: 19, amount: 2250, platformFee: 250,
    status: 'COMPLETED', createdAt: '2024-03-12T10:00:00Z', processedAt: '2024-03-12T15:00:00Z',
  },
  {
    id: 418, contractId: 325, freelancerId: 20, amount: 1620, platformFee: 180,
    status: 'COMPLETED', createdAt: '2024-06-02T10:00:00Z', processedAt: '2024-06-02T14:00:00Z',
  },
  // Pending payouts (active contracts)
  {
    id: 419, contractId: 312, freelancerId: 11, amount: 3150, platformFee: 350,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  {
    id: 420, contractId: 313, freelancerId: 12, amount: 6480, platformFee: 720,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  {
    id: 421, contractId: 315, freelancerId: 14, amount: 6120, platformFee: 680,
    status: 'PENDING', createdAt: '2024-06-14T08:00:00Z',
  },
  // Failed payout (compensation refunded)
  {
    id: 422, contractId: 324, freelancerId: 3, amount: 0, platformFee: 0,
    status: 'REFUNDED', createdAt: '2024-05-22T14:00:00Z', processedAt: '2024-05-23T09:00:00Z',
    transactionDetails: { reason: 'Contract terminated — saga compensation refund issued' },
  },
]

// Bob's payout summary (accumulated career earnings on the platform)
export const mockPayoutSummary: FreelancerPayoutSummary = {
  freelancerId: 2,
  totalPaid: 68420,
  totalPending: 15120,
  totalFailed: 1200,
  totalRefunded: 0,
  totalPlatformFees: 7602,
  payoutCount: 23,
}

// ─── Platform Analytics ──────────────────────────────────────────────────────

export const mockPlatformFees: PlatformFeeAnalytics[] = [
  { category: 'Backend',       totalFees: 28400, totalVolume: 284000, payoutCount: 48, averageFeeRate: 0.10 },
  { category: 'Frontend',      totalFees: 18700, totalVolume: 187000, payoutCount: 32, averageFeeRate: 0.10 },
  { category: 'Mobile',        totalFees: 15200, totalVolume: 152000, payoutCount: 24, averageFeeRate: 0.10 },
  { category: 'Data Science',  totalFees: 12900, totalVolume: 129000, payoutCount: 19, averageFeeRate: 0.10 },
  { category: 'Design',        totalFees: 7640,  totalVolume: 76400,  payoutCount: 21, averageFeeRate: 0.10 },
  { category: 'DevOps',        totalFees: 9820,  totalVolume: 98200,  payoutCount: 14, averageFeeRate: 0.10 },
  { category: 'Blockchain',    totalFees: 6200,  totalVolume: 62000,  payoutCount: 8,  averageFeeRate: 0.10 },
  { category: 'Security',      totalFees: 5580,  totalVolume: 55800,  payoutCount: 9,  averageFeeRate: 0.10 },
]

export const mockPromoCodes: PromoCode[] = [
  {
    id: 1, code: 'LAUNCH20', discountPercent: 20, usageCount: 214,
    maxUsage: 500, expiresAt: '2026-09-30T23:59:59Z', active: true,
  },
  {
    id: 2, code: 'SUMMER10', discountPercent: 10, usageCount: 87,
    maxUsage: 200, expiresAt: '2026-08-31T23:59:59Z', active: true,
  },
  {
    id: 3, code: 'DEVFEST25', discountPercent: 25, usageCount: 43,
    maxUsage: 100, expiresAt: '2026-07-15T23:59:59Z', active: true,
  },
  {
    id: 4, code: 'FREELANCE15', discountPercent: 15, usageCount: 156,
    maxUsage: 300, expiresAt: '2026-12-31T23:59:59Z', active: true,
  },
  {
    id: 5, code: 'WELCOME50', discountPercent: 50, usageCount: 29,
    maxUsage: 30, expiresAt: '2026-06-30T23:59:59Z', active: true,
  },
  {
    id: 6, code: 'ENTERPRISE10', discountPercent: 10, usageCount: 8,
    maxUsage: 50, expiresAt: '2027-01-31T23:59:59Z', active: true,
  },
]

// ─── Admin Dashboard ─────────────────────────────────────────────────────────

export const mockAdminDashboard: AdminDashboard = {
  totalUsers:          1247,
  totalJobs:           3892,
  totalContracts:      2156,
  totalRevenue:        847320,
  activeContracts:     384,
  openJobs:            512,
  pendingPayouts:      47,
  platformFeeCollected: 84732,
}

export const mockTopFreelancers: TopFreelancer[] = [
  { freelancerId: 2,  name: 'Bob Martinez',    totalEarnings: 68420, completedContracts: 23, completionRate: 0.92, rating: 4.9 },
  { freelancerId: 12, name: 'Karim El-Amin',   totalEarnings: 54800, completedContracts: 18, completionRate: 0.95, rating: 4.9 },
  { freelancerId: 9,  name: 'Hiroshi Tanaka',  totalEarnings: 49200, completedContracts: 16, completionRate: 0.94, rating: 4.8 },
  { freelancerId: 17, name: 'Priya Sharma',    totalEarnings: 43600, completedContracts: 14, completionRate: 0.93, rating: 4.9 },
  { freelancerId: 14, name: 'Marco Oliveira',  totalEarnings: 38900, completedContracts: 12, completionRate: 0.91, rating: 4.8 },
  { freelancerId: 11, name: 'James Wilson',    totalEarnings: 36100, completedContracts: 15, completionRate: 0.88, rating: 4.8 },
  { freelancerId: 3,  name: 'Carol White',     totalEarnings: 28400, completedContracts: 19, completionRate: 0.90, rating: 4.7 },
  { freelancerId: 20, name: 'Tyler Brooks',    totalEarnings: 22800, completedContracts: 9,  completionRate: 0.89, rating: 4.8 },
]

export const mockContractAnalytics: ContractAnalytics = {
  totalContracts:  2156,
  avgValue:        5842,
  completionRate:  0.87,
  avgDurationDays: 31,
  byStatus:        { ACTIVE: 384, COMPLETED: 1645, TERMINATED: 127 },
}

// ─── Saga Timeline ───────────────────────────────────────────────────────────

export const mockSagaSteps: SagaStep[] = [
  {
    id: 's1', event: 'PROPOSAL_SUBMITTED', label: 'Proposal Submitted',
    description: 'Bob submitted proposal #203 for the DevOps setup job with a bid of $3,900.',
    timestamp: '2024-06-09T08:00:00Z', status: 'completed',
    metadata: { proposalId: 203, bidAmount: 3900, freelancerId: 2 },
  },
  {
    id: 's2', event: 'PROPOSAL_ACCEPTED', label: 'Proposal Accepted',
    description: 'Alice accepted proposal #203. Event published to freelance.events exchange.',
    timestamp: '2024-06-09T14:00:00Z', status: 'completed',
    metadata: { proposalId: 203, clientId: 1, exchange: 'freelance.events' },
  },
  {
    id: 's3', event: 'CONTRACT_CREATED', label: 'Contract Created',
    description: 'contract-service consumed PROPOSAL_ACCEPTED and created Contract #303 automatically.',
    timestamp: '2024-06-09T14:02:00Z', status: 'completed',
    metadata: { contractId: 303, agreedAmount: 3900, sagaStep: 'choreography' },
  },
  {
    id: 's4', event: 'PROPOSAL_COMPLETING', label: 'Work Submitted',
    description: 'Bob marked the work as complete. PROPOSAL_COMPLETING event published to wallet-service.',
    timestamp: '2024-06-14T09:00:00Z', status: 'active',
    metadata: { proposalId: 203, contractId: 303 },
  },
  {
    id: 's5', event: 'PAYMENT_INITIATED', label: 'Payment Initiated',
    description: 'wallet-service initiating payout of $3,510 (90% of $3,900 after platform fee).',
    timestamp: '', status: 'pending',
    metadata: { amount: 3510, platformFee: 390 },
  },
  {
    id: 's6', event: 'PAYMENT_COMPLETED', label: 'Payment Completed',
    description: 'Payout transferred to Bob. Contract #303 marked COMPLETED by contract-service.',
    timestamp: '', status: 'pending',
  },
]

// ─── Chart Data ───────────────────────────────────────────────────────────────

export const mockRevenueChart = [
  { month: "Jan '24", revenue: 38200,  contracts: 42 },
  { month: "Feb '24", revenue: 44500,  contracts: 51 },
  { month: "Mar '24", revenue: 41800,  contracts: 47 },
  { month: "Apr '24", revenue: 52300,  contracts: 63 },
  { month: "May '24", revenue: 58900,  contracts: 71 },
  { month: "Jun '24", revenue: 67400,  contracts: 82 },
  { month: "Jul '24", revenue: 61200,  contracts: 74 },
  { month: "Aug '24", revenue: 72800,  contracts: 89 },
  { month: "Sep '24", revenue: 84500,  contracts: 103 },
  { month: "Oct '24", revenue: 91200,  contracts: 112 },
  { month: "Nov '24", revenue: 88400,  contracts: 107 },
  { month: "Dec '24", revenue: 74100,  contracts: 90 },
  { month: "Jan '25", revenue: 79600,  contracts: 97 },
  { month: "Feb '25", revenue: 93800,  contracts: 114 },
  { month: "Mar '25", revenue: 108400, contracts: 132 },
  { month: "Apr '25", revenue: 124700, contracts: 152 },
  { month: "May '25", revenue: 138200, contracts: 168 },
  { month: "Jun '25", revenue: 147500, contracts: 180 },
]

export const mockProposalStatusData = [
  { name: 'Accepted',  value: 312,  fill: '#6366f1' },
  { name: 'Submitted', value: 528,  fill: '#f59e0b' },
  { name: 'Paid',      value: 984,  fill: '#10b981' },
  { name: 'Rejected',  value: 418,  fill: '#ef4444' },
  { name: 'Withdrawn', value: 97,   fill: '#94a3b8' },
  { name: 'Shortlisted', value: 213, fill: '#8b5cf6' },
]
