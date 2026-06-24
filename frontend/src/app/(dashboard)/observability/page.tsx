'use client'

import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { ExternalLink, Activity, BarChart3, Search, Database } from 'lucide-react'

const TOOLS = [
  {
    icon: BarChart3,
    name: 'Grafana',
    desc: 'Dashboards for service health, JVM metrics, RabbitMQ queue depths, and API latency.',
    url:  process.env.NEXT_PUBLIC_GRAFANA_URL ?? 'http://localhost:3001',
    color: 'text-orange-500 bg-orange-50 dark:bg-orange-900/20',
  },
  {
    icon: Activity,
    name: 'Prometheus',
    desc: 'Metrics scraping for all Spring Boot Actuator /metrics endpoints.',
    url:  process.env.NEXT_PUBLIC_PROMETHEUS_URL ?? 'http://localhost:9090',
    color: 'text-red-500 bg-red-50 dark:bg-red-900/20',
  },
  {
    icon: Search,
    name: 'Kibana (ELK)',
    desc: 'Centralised log aggregation from all microservices via Logstash + Elasticsearch.',
    url:  process.env.NEXT_PUBLIC_KIBANA_URL ?? 'http://localhost:5601',
    color: 'text-brand-500 bg-brand-50 dark:bg-brand-900/20',
  },
  {
    icon: Database,
    name: 'RabbitMQ Management',
    desc: 'Monitor exchanges, queues, message rates, and dead-letter queue depth.',
    url:  process.env.NEXT_PUBLIC_RABBITMQ_URL ?? 'http://localhost:15672',
    color: 'text-amber-500 bg-amber-50 dark:bg-amber-900/20',
  },
]

const ACTUATOR_ENDPOINTS = [
  { path: '/actuator/health',  desc: 'Service readiness / liveness' },
  { path: '/actuator/metrics', desc: 'JVM, HTTP, and custom metrics' },
  { path: '/actuator/info',    desc: 'Build & version metadata' },
  { path: '/actuator/env',     desc: 'Environment properties' },
]

export default function ObservabilityPage() {
  return (
    <DashboardLayout title="Observability">
      <div className="space-y-6">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          All services expose Spring Boot Actuator endpoints. The full observability stack
          (Prometheus + Grafana + ELK) runs in Kubernetes via Helm charts deployed by{' '}
          <code className="rounded bg-gray-100 px-1 text-xs dark:bg-gray-800">setup.bash</code>.
        </p>

        {/* Tool cards */}
        <div className="grid gap-4 sm:grid-cols-2">
          {TOOLS.map(({ icon: Icon, name, desc, url, color }) => (
            <Card key={name} className="hover:shadow-md transition-shadow">
              <CardContent className="p-5">
                <div className="flex items-start gap-4">
                  <div className={`rounded-lg p-2.5 ${color}`}>
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="flex-1">
                    <p className="font-semibold text-gray-900 dark:text-white">{name}</p>
                    <p className="mt-1 text-sm text-gray-500">{desc}</p>
                    <a href={url} target="_blank" rel="noreferrer" className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-brand-600 hover:underline">
                      Open {name} <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Actuator */}
        <Card>
          <CardHeader>
            <CardTitle>Spring Boot Actuator Endpoints</CardTitle>
            <CardDescription>Available on management port 8081 per service</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-2 sm:grid-cols-2">
              {ACTUATOR_ENDPOINTS.map((e) => (
                <div key={e.path} className="flex items-center gap-3 rounded-lg border border-gray-100 p-3 dark:border-gray-700">
                  <code className="text-xs font-mono text-brand-600 dark:text-brand-400">{e.path}</code>
                  <span className="text-xs text-gray-500">{e.desc}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Service ports */}
        <Card>
          <CardHeader><CardTitle>Service Port Map</CardTitle></CardHeader>
          <CardContent>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 dark:border-gray-700">
                  <th className="pb-2 text-left text-xs font-semibold uppercase text-gray-400">Service</th>
                  <th className="pb-2 text-left text-xs font-semibold uppercase text-gray-400">App Port</th>
                  <th className="pb-2 text-left text-xs font-semibold uppercase text-gray-400">Management</th>
                  <th className="pb-2 text-left text-xs font-semibold uppercase text-gray-400">DB</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
                {[
                  { svc: 'user-service',      app: '8081', mgmt: '9081', db: 'PostgreSQL' },
                  { svc: 'job-service',       app: '8082', mgmt: '9082', db: 'MongoDB + Elasticsearch' },
                  { svc: 'proposal-service',  app: '8083', mgmt: '9083', db: 'Cassandra' },
                  { svc: 'contract-service',  app: '8084', mgmt: '9084', db: 'Cassandra' },
                  { svc: 'wallet-service',    app: '8085', mgmt: '9085', db: 'PostgreSQL + Redis' },
                ].map((r) => (
                  <tr key={r.svc} className="text-gray-600 dark:text-gray-400">
                    <td className="py-2 font-mono text-xs">{r.svc}</td>
                    <td className="py-2">{r.app}</td>
                    <td className="py-2">{r.mgmt}</td>
                    <td className="py-2">{r.db}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
