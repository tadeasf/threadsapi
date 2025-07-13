"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"

import {
    User,
    Users,
    FileText,
    Eye,
    Heart,
    ExternalLink,
    RefreshCw
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface UserProfile {
    id: string
    username: string
    name: string
    threads_profile_picture_url?: string
    threads_biography?: string
}

interface PostStatistics {
    totalPosts: number
    totalViews: number
    totalLikes: number
}

interface UserInsights {
    currentMetrics: {
        views?: number
        followers_count?: number
    }
}

export default function ProfilePage() {
    const [userProfile, setUserProfile] = useState<UserProfile | null>(null)
    const [postStats, setPostStats] = useState<PostStatistics | null>(null)
    const [userInsights, setUserInsights] = useState<UserInsights | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] = useState(false)
    const router = useRouter()

    useEffect(() => {
        fetchUserData()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const fetchUserData = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            setIsRefreshing(true)

            // Fetch user profile
            const profileResponse = await fetch(`${API_BASE_URL}/api/user/profile?accessToken=${token}`)
            if (profileResponse.ok) {
                const profile = await profileResponse.json()
                setUserProfile(profile)
            }

            // Fetch post statistics
            const statsResponse = await fetch(`${API_BASE_URL}/api/posts/user/${userId}/statistics`)
            if (statsResponse.ok) {
                const stats = await statsResponse.json()
                setPostStats(stats)
            }

            // Fetch user insights
            const insightsResponse = await fetch(`${API_BASE_URL}/api/insights/dashboard/${userId}?accessToken=${token}`)
            if (insightsResponse.ok) {
                const insights = await insightsResponse.json()
                setUserInsights(insights)
            }

        } catch (error) {
            console.error('Error fetching user data:', error)
        } finally {
            setIsLoading(false)
            setIsRefreshing(false)
        }
    }

    const formatNumber = (num: number) => {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M'
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K'
        }
        return num.toString()
    }

    const handleViewOnThreads = () => {
        if (userProfile?.username) {
            window.open(`https://threads.net/@${userProfile.username}`, '_blank')
        }
    }

    if (isLoading) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="container mx-auto px-4 py-8">
                    <div className="animate-pulse space-y-4">
                        <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                        <div className="h-32 bg-gray-200 rounded"></div>
                        <div className="h-64 bg-gray-200 rounded"></div>
                    </div>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Profile</h1>
                        <p className="text-gray-600 mt-2">Your Threads profile information and statistics</p>
                    </div>
                    <Button onClick={fetchUserData} variant="outline" disabled={isRefreshing}>
                        <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
                        Refresh Data
                    </Button>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Profile Information */}
                    <div className="lg:col-span-2 space-y-6">
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <User className="h-5 w-5 mr-2" />
                                    Profile Information
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="flex items-start space-x-4">
                                    <Avatar className="h-20 w-20">
                                        <AvatarImage
                                            src={userProfile?.threads_profile_picture_url}
                                            alt={userProfile?.username || 'User'}
                                        />
                                        <AvatarFallback className="text-lg">
                                            {userProfile?.username?.charAt(0).toUpperCase() || 'U'}
                                        </AvatarFallback>
                                    </Avatar>
                                    <div className="flex-1 space-y-2">
                                        <div>
                                            <h3 className="text-xl font-semibold">
                                                {userProfile?.name || userProfile?.username}
                                            </h3>
                                            <p className="text-gray-600">@{userProfile?.username}</p>
                                        </div>
                                        {userProfile?.threads_biography && (
                                            <p className="text-gray-700 leading-relaxed">
                                                {userProfile.threads_biography}
                                            </p>
                                        )}
                                        <div className="flex items-center space-x-4 pt-2">
                                            <Button
                                                onClick={handleViewOnThreads}
                                                variant="outline"
                                                size="sm"
                                            >
                                                <ExternalLink className="h-4 w-4 mr-2" />
                                                View on Threads
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Account Details */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Account Details</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                        <label className="text-sm font-medium text-gray-500">User ID</label>
                                        <p className="font-mono text-sm bg-gray-100 p-2 rounded">
                                            {userProfile?.id}
                                        </p>
                                    </div>
                                    <div>
                                        <label className="text-sm font-medium text-gray-500">Username</label>
                                        <p className="font-mono text-sm bg-gray-100 p-2 rounded">
                                            @{userProfile?.username}
                                        </p>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Statistics Sidebar */}
                    <div className="space-y-6">
                        {/* Post Statistics */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <FileText className="h-5 w-5 mr-2" />
                                    Post Statistics
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <div className="space-y-3">
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center">
                                            <FileText className="h-4 w-4 mr-2 text-blue-500" />
                                            <span className="text-sm">Total Posts</span>
                                        </div>
                                        <Badge variant="secondary">
                                            {formatNumber(postStats?.totalPosts || 0)}
                                        </Badge>
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center">
                                            <Eye className="h-4 w-4 mr-2 text-green-500" />
                                            <span className="text-sm">Total Views</span>
                                        </div>
                                        <Badge variant="secondary">
                                            {formatNumber(postStats?.totalViews || 0)}
                                        </Badge>
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center">
                                            <Heart className="h-4 w-4 mr-2 text-red-500" />
                                            <span className="text-sm">Total Likes</span>
                                        </div>
                                        <Badge variant="secondary">
                                            {formatNumber(postStats?.totalLikes || 0)}
                                        </Badge>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Insights Summary */}
                        {userInsights && (
                            <Card>
                                <CardHeader>
                                    <CardTitle className="flex items-center">
                                        <Eye className="h-5 w-5 mr-2" />
                                        Insights Summary
                                    </CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="space-y-3">
                                        {userInsights.currentMetrics.views !== undefined && (
                                            <div className="flex items-center justify-between">
                                                <div className="flex items-center">
                                                    <Eye className="h-4 w-4 mr-2 text-blue-500" />
                                                    <span className="text-sm">Profile Views</span>
                                                </div>
                                                <Badge variant="secondary">
                                                    {formatNumber(userInsights.currentMetrics.views)}
                                                </Badge>
                                            </div>
                                        )}
                                        {userInsights.currentMetrics.followers_count !== undefined && (
                                            <div className="flex items-center justify-between">
                                                <div className="flex items-center">
                                                    <Users className="h-4 w-4 mr-2 text-purple-500" />
                                                    <span className="text-sm">Followers</span>
                                                </div>
                                                <Badge variant="secondary">
                                                    {formatNumber(userInsights.currentMetrics.followers_count)}
                                                </Badge>
                                            </div>
                                        )}
                                    </div>
                                </CardContent>
                            </Card>
                        )}

                        {/* Quick Actions */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Quick Actions</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-3">
                                <Button
                                    onClick={() => router.push('/posts')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <FileText className="h-4 w-4 mr-2" />
                                    Manage Posts
                                </Button>
                                <Button
                                    onClick={() => router.push('/insights')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <Eye className="h-4 w-4 mr-2" />
                                    View Insights
                                </Button>
                                <Button
                                    onClick={() => router.push('/search')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <FileText className="h-4 w-4 mr-2" />
                                    Search Posts
                                </Button>
                                <Button
                                    onClick={() => router.push('/settings')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <User className="h-4 w-4 mr-2" />
                                    Account Settings
                                </Button>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    )
} 