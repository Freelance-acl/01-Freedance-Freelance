/** @type {import('next').NextConfig} */
const nextConfig = {
  distDir: process.env.NEXT_DIST_DIR || '.next',
  async rewrites() {
    const gatewayUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:30080'
    return [
      {
        source: '/api/:path*',
        destination: `${gatewayUrl}/api/:path*`,
      },
    ]
  },
  images: {
    domains: ['api.dicebear.com'],
  },
}

export default nextConfig
