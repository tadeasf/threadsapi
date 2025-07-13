"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default function Home() {
  const router = useRouter()

  useEffect(() => {
    // Check if user is already authenticated
    const token = localStorage.getItem('threads_access_token')
    if (token) {
      router.push('/dashboard')
    }
  }, [router])

  const handleGetStarted = () => {
    router.push('/login')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="max-w-4xl mx-auto text-center space-y-8">
        {/* Hero Section */}
        <div className="space-y-4">
          <h1 className="text-4xl md:text-6xl font-bold text-gray-900">
            Threads API Wrapper
          </h1>
          <p className="text-xl md:text-2xl text-gray-600 max-w-2xl mx-auto">
            A comprehensive Spring Boot wrapper for Meta&apos;s Threads API with a modern React frontend
          </p>
        </div>

        {/* Feature Cards */}
        <div className="grid md:grid-cols-3 gap-6 mt-12">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <span>🔐</span>
                <span>OAuth Authentication</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Secure Meta OAuth flow with automatic token exchange and refresh
              </CardDescription>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <span>📝</span>
                <span>Post Creation</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Create and publish text posts with optional images to Threads
              </CardDescription>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <span>🛠️</span>
                <span>REST API</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>
                Full REST API with OpenAPI documentation and Swagger UI
              </CardDescription>
            </CardContent>
          </Card>
        </div>

        {/* Technology Stack */}
        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Technology Stack</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid md:grid-cols-2 gap-6 text-left">
              <div>
                <h4 className="font-semibold mb-2">Backend</h4>
                <ul className="space-y-1 text-sm text-gray-600">
                  <li>• Spring Boot 3.5.x</li>
                  <li>• Java 21 LTS</li>
                  <li>• SpringDoc OpenAPI</li>
                  <li>• Spring Web MVC</li>
                  <li>• Environment Variables Support</li>
                </ul>
              </div>
              <div>
                <h4 className="font-semibold mb-2">Frontend</h4>
                <ul className="space-y-1 text-sm text-gray-600">
                  <li>• Next.js 15 with App Router</li>
                  <li>• React 19</li>
                  <li>• Shadcn/ui Components</li>
                  <li>• Tailwind CSS</li>
                  <li>• TypeScript</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* API Endpoints */}
        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Available API Endpoints</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-left space-y-2 text-sm">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <h4 className="font-semibold text-green-600 mb-2">Authentication</h4>
                  <ul className="space-y-1 text-gray-600">
                    <li>GET /api/auth/login-url</li>
                    <li>POST /api/auth/exchange-token</li>
                    <li>GET /api/auth/callback</li>
                    <li>POST /api/auth/refresh-token</li>
                  </ul>
                </div>
                <div>
                  <h4 className="font-semibold text-blue-600 mb-2">Content & User</h4>
                  <ul className="space-y-1 text-gray-600">
                    <li>POST /api/posts/create</li>
                    <li>POST /api/posts/publish</li>
                    <li>GET /api/user/profile</li>
                    <li>GET /api/user/threads</li>
                  </ul>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* CTA */}
        <div className="space-y-4 pt-8">
          <Button onClick={handleGetStarted} size="lg" className="text-lg px-8 py-3">
            Get Started
          </Button>
          <p className="text-sm text-gray-500">
            Connect your Meta account and start posting to Threads
          </p>
        </div>

        {/* Footer */}
        <div className="pt-8 text-sm text-gray-500 space-y-2">
          <p>
            API Documentation: <code className="bg-gray-200 px-2 py-1 rounded">http://localhost:10081/swagger-ui.html</code>
          </p>
          <p>
            Backend API: <code className="bg-gray-200 px-2 py-1 rounded">http://localhost:10081</code>
          </p>
        </div>
      </div>
    </div>
  )
}
