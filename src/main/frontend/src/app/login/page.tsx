"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useRouter, useSearchParams } from "next/navigation"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

export default function LoginPage() {
    const [isLoading, setIsLoading] = useState(false)
    const [authUrl, setAuthUrl] = useState<string | null>(null)
    const [redirectUri, setRedirectUri] = useState<string | null>(null)
    const router = useRouter()
    const searchParams = useSearchParams()

    useEffect(() => {
        // Check if we have tokens from the OAuth callback
        const success = searchParams.get('success')
        const accessToken = searchParams.get('access_token')
        const userId = searchParams.get('user_id')
        const error = searchParams.get('error')

        if (error) {
            console.error('OAuth error:', error)
            alert(`Authentication failed: ${error}`)
            // Clear URL parameters and fetch login URL
            window.history.replaceState({}, '', '/login')
            fetchLoginUrl()
        } else if (success === 'true' && accessToken && userId) {
            // Store tokens and redirect to dashboard
            localStorage.setItem('threads_access_token', accessToken)
            localStorage.setItem('threads_user_id', userId)
            router.push('/dashboard')
        } else {
            // No callback parameters, fetch the login URL
            fetchLoginUrl()
        }
    }, [searchParams, router])

    const fetchLoginUrl = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/login-url`)
            const data = await response.json()
            setAuthUrl(data.authUrl)
            setRedirectUri(data.redirectUri)
        } catch (error) {
            console.error('Failed to fetch login URL:', error)
        }
    }



    const handleLogin = () => {
        if (authUrl) {
            window.location.href = authUrl
        }
    }

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <Card className="w-full max-w-md">
                    <CardContent className="pt-6">
                        <div className="text-center">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto"></div>
                            <p className="mt-4">Authenticating with Meta...</p>
                        </div>
                    </CardContent>
                </Card>
            </div>
        )
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <Card className="w-full max-w-md">
                <CardHeader className="text-center">
                    <CardTitle className="text-2xl font-bold">Threads API Wrapper</CardTitle>
                    <CardDescription>
                        Connect your Meta account to start posting to Threads
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="text-center space-y-4">
                        <p className="text-sm text-gray-600">
                            You'll be redirected to Meta to authorize this application
                        </p>

                        <Button
                            onClick={handleLogin}
                            disabled={!authUrl}
                            className="w-full"
                            size="lg"
                        >
                            {authUrl ? 'Connect with Meta' : 'Loading...'}
                        </Button>

                        <div className="text-xs text-gray-500 space-y-1">
                            <p>This app requires the following permissions:</p>
                            <ul className="list-disc list-inside text-left">
                                <li>threads_basic - Access basic profile info and posts</li>
                                <li>threads_content_publish - Create and publish posts</li>
                                <li>threads_delete - Delete posts</li>
                                <li>threads_keyword_search - Search posts by keywords</li>
                                <li>threads_location_tagging - Tag posts with locations</li>
                                <li>threads_manage_insights - Access analytics and insights</li>
                                <li>threads_manage_mentions - Manage mentions and notifications</li>
                                <li>threads_manage_replies - Manage replies and conversations</li>
                                <li>threads_read_replies - Read replies and conversations</li>
                            </ul>
                        </div>

                        {/* Debug info for development */}
                        {process.env.NODE_ENV === 'development' && redirectUri && (
                            <div className="text-xs text-gray-400 mt-4 p-2 bg-gray-100 rounded">
                                <p><strong>Debug Info:</strong></p>
                                <p>Redirect URI: {redirectUri}</p>
                                <p>API Base: {API_BASE_URL}</p>
                                <p>Current Origin: {typeof window !== 'undefined' ? window.location.origin : 'N/A'}</p>
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
        </div>
    )
} 