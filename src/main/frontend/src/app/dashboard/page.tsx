"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import Header from '@/components/Header'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Alert, AlertDescription } from "@/components/ui/alert"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

const postSchema = z.object({
    text: z.string().min(1, "Post text is required").max(500, "Post text must be 500 characters or less"),
    imageUrl: z.string().url("Must be a valid URL").optional().or(z.literal("")),
})

type PostFormData = z.infer<typeof postSchema>

interface UserProfile {
    id: string
    username: string
    name: string
    threads_profile_picture_url?: string
    threads_biography?: string
}

export default function DashboardPage() {
    const [isLoading, setIsLoading] = useState(false)
    const [userProfile, setUserProfile] = useState<UserProfile | null>(null)
    const [accessToken, setAccessToken] = useState<string | null>(null)
    const [isTokenValid, setIsTokenValid] = useState(true)
    const router = useRouter()

    const form = useForm<PostFormData>({
        resolver: zodResolver(postSchema),
        defaultValues: {
            text: "",
            imageUrl: "",
        },
    })

    const fetchUserProfile = useCallback(async (token: string) => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/user/profile?accessToken=${token}`)
            if (response.ok) {
                const profile = await response.json()
                setUserProfile(profile)
                setIsTokenValid(true)
            } else {
                console.error('Failed to fetch user profile')
                setIsTokenValid(false)
                // Token might be expired or revoked, redirect to login
                localStorage.removeItem('threads_access_token')
                localStorage.removeItem('threads_user_id')
                router.push('/login')
            }
        } catch (error) {
            console.error('Error fetching user profile:', error)
            setIsTokenValid(false)
        }
    }, [router])

    useEffect(() => {
        // Check if user is authenticated
        const token = localStorage.getItem('threads_access_token')
        if (!token) {
            router.push('/login')
            return
        }

        setAccessToken(token)
        fetchUserProfile(token)
    }, [router, fetchUserProfile])

    const onSubmit = async (data: PostFormData) => {
        if (!accessToken) return

        setIsLoading(true)
        try {
            // Create the post
            const createResponse = await fetch(`${API_BASE_URL}/api/posts/create?accessToken=${accessToken}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    media_type: data.imageUrl ? 'IMAGE' : 'TEXT',
                    text: data.text,
                    image_url: data.imageUrl || undefined,
                }),
            })

            if (createResponse.ok) {
                const createResult = await createResponse.json()

                // Publish the post
                const publishResponse = await fetch(`${API_BASE_URL}/api/posts/publish?accessToken=${accessToken}&creationId=${createResult.id}`, {
                    method: 'POST',
                })

                if (publishResponse.ok) {
                    const publishResult = await publishResponse.json()
                    alert(`Post published successfully! Post ID: ${publishResult.id}`)
                    form.reset()
                } else {
                    alert('Post created but failed to publish. Check the creation ID in the console.')
                    console.log('Creation ID:', createResult.id)
                }
            } else {
                const errorText = await createResponse.text()
                if (createResponse.status === 401) {
                    // Token expired or revoked
                    setIsTokenValid(false)
                    alert('Your session has expired. Please log in again.')
                    handleLogout()
                } else {
                    alert(`Failed to create post: ${errorText}`)
                }
            }
        } catch (error) {
            console.error('Error creating post:', error)
            alert('Failed to create post. Check console for details.')
        } finally {
            setIsLoading(false)
        }
    }

    const handleLogout = () => {
        localStorage.removeItem('threads_access_token')
        localStorage.removeItem('threads_user_id')
        router.push('/login')
    }

    if (!userProfile) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />
            <div className="max-w-4xl mx-auto px-4 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Welcome back!</h1>
                    <p className="text-gray-600">Create and manage your Threads posts</p>
                </div>

                {!isTokenValid && (
                    <Alert className="mb-6 border-red-200 bg-red-50">
                        <AlertDescription className="text-red-800">
                            Your access token appears to be invalid or expired. You may have revoked permissions
                            for this app in your Meta account settings. Please log in again to continue.
                        </AlertDescription>
                    </Alert>
                )}

                {/* User Profile Card */}
                <Card className="mb-8">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-4">
                            {userProfile.threads_profile_picture_url && (
                                <Image
                                    src={userProfile.threads_profile_picture_url}
                                    alt={userProfile.name || userProfile.username}
                                    width={48}
                                    height={48}
                                    className="rounded-full"
                                />
                            )}
                            <div>
                                <div className="text-lg font-semibold">{userProfile.name}</div>
                                <div className="text-sm text-gray-500">@{userProfile.username}</div>
                            </div>
                        </CardTitle>
                        {userProfile.threads_biography && (
                            <CardDescription>{userProfile.threads_biography}</CardDescription>
                        )}
                    </CardHeader>
                </Card>

                {/* Create Post Card */}
                <Card className="mb-8">
                    <CardHeader>
                        <CardTitle>Create New Post</CardTitle>
                        <CardDescription>
                            Share your thoughts on Threads
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Form {...form}>
                            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                                <FormField
                                    control={form.control}
                                    name="text"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Post Text</FormLabel>
                                            <FormControl>
                                                <Textarea
                                                    placeholder="What's on your mind?"
                                                    className="min-h-[100px]"
                                                    {...field}
                                                />
                                            </FormControl>
                                            <FormDescription>
                                                Share your thoughts (max 500 characters)
                                            </FormDescription>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name="imageUrl"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Image URL (Optional)</FormLabel>
                                            <FormControl>
                                                <Input
                                                    placeholder="https://example.com/image.jpg"
                                                    {...field}
                                                />
                                            </FormControl>
                                            <FormDescription>
                                                Add an image to your post
                                            </FormDescription>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <Button
                                    type="submit"
                                    disabled={isLoading || !isTokenValid}
                                    className="w-full"
                                >
                                    {isLoading ? "Publishing..." : "Publish Post"}
                                </Button>
                            </form>
                        </Form>
                    </CardContent>
                </Card>

                {/* Account Status Card */}
                <Card className="mb-8">
                    <CardHeader>
                        <CardTitle>Account Status</CardTitle>
                        <CardDescription>Your Threads API integration status</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex items-center justify-between">
                            <span className="text-sm font-medium">Connection Status</span>
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${isTokenValid
                                ? 'bg-green-100 text-green-800'
                                : 'bg-red-100 text-red-800'
                                }`}>
                                {isTokenValid ? 'Connected' : 'Disconnected'}
                            </span>
                        </div>

                        <div className="text-sm text-gray-600">
                            <p><strong>User ID:</strong> {userProfile.id}</p>
                            <p><strong>Username:</strong> @{userProfile.username}</p>
                        </div>

                        <div className="border-t pt-4">
                            <h4 className="text-sm font-medium mb-2">Webhook Information</h4>
                            <p className="text-xs text-gray-500">
                                If you revoke permissions for this app in your Meta account settings,
                                our system will automatically be notified and your data will be handled
                                according to our privacy policy.
                            </p>
                        </div>
                    </CardContent>
                </Card>

                {/* API Information */}
                <Card className="mt-6">
                    <CardHeader>
                        <CardTitle className="text-lg">API Information</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                        <div className="text-sm">
                            <p><strong>User ID:</strong> {userProfile.id}</p>
                            <p><strong>API Base URL:</strong> {API_BASE_URL}</p>
                            <p><strong>Available Endpoints:</strong></p>
                            <ul className="list-disc list-inside ml-4 space-y-1">
                                <li>GET /api/auth/login-url</li>
                                <li>POST /api/auth/exchange-token</li>
                                <li>GET /api/user/profile</li>
                                <li>POST /api/posts/create</li>
                                <li>POST /api/posts/publish</li>
                                <li>GET /api/user/threads</li>
                                <li>POST /api/auth/uninstall (webhook)</li>
                                <li>POST /api/auth/delete (webhook)</li>
                            </ul>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
} 