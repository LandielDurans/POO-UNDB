/** @type {import('next').NextConfig} */

// Endereço do backend Spring Boot. Em dev, padrão é localhost:8080.
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig = {
  // Proxy: o navegador chama /api/... no próprio Next, e o Next repassa
  // para o backend. Assim não precisamos de CORS no Spring durante o dev.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${BACKEND_URL}/api/:path*` }];
  },
};

export default nextConfig;
