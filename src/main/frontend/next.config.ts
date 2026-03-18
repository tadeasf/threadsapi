import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone", // Optimizes for Docker deployment
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:10081',
  },
};

export default nextConfig;
