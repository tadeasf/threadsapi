"use client"

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
    ChartLegend,
    ChartLegendContent,
} from "@/components/ui/chart"
import { Bar, BarChart, XAxis, YAxis, CartesianGrid, PieChart, Pie, Cell, Area, AreaChart } from 'recharts'
import {
    TrendingUp,
    Eye,
    Heart,
    MessageCircle,
    RefreshCw,
    BarChart3,
    Activity,
    Users,
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface InsightsDashboard {
    userMetrics: {
        views?: number
        likes?: number
        replies?: number
        quotes?: number
        clicks?: number
        followers_count?: number
    }
    totalMediaMetrics: {
        total_views: number
        total_likes: number
        total_replies: number
        total_reposts: number
        total_quotes: number
    }
    averageMetrics: {
        avg_views: number
        avg_likes: number
        avg_replies: number
        avg_reposts: number
        avg_quotes: number
    }
    topPosts: Array<{
        id: string
        text: string
        views: number
        likes: number
        replies: number
        timestamp: string
    }>
    engagementTrends: Array<{
        date: string
        views: number
        likes: number
        replies: number
        reposts: number
        quotes: number
    }>
    totalPosts: number
}

const chartConfig = {
    views: {
        label: "Views",
        color: "#2563eb",
    },
    likes: {
        label: "Likes",
        color: "#dc2626",
    },
    replies: {
        label: "Replies",
        color: "#16a34a",
    },
    reposts: {
        label: "Reposts",
        color: "#ca8a04",
    },
    quotes: {
        label: "Quotes",
        color: "#9333ea",
    },
    shares: {
        label: "Shares",
        color: "#ea580c",
    },
}

const COLORS = ['#2563eb', '#dc2626', '#16a34a', '#ca8a04', '#9333ea', '#ea580c']

export default function InsightsPage() {
    const [dashboard, setDashboard] = useState<InsightsDashboard | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] = useState(false)
    const router = useRouter()

    const fetchInsightsData = useCallback(async () => {
        setIsLoading(true)
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            // Fetch comprehensive dashboard data
            const dashboardRes = await fetch(`${API_BASE_URL}/api/insights/dashboard/${userId}?accessToken=${token}`)

            if (dashboardRes.ok) {
                const dashboardData = await dashboardRes.json()
                setDashboard(dashboardData)
            }

        } catch (error) {
            console.error('Error fetching insights data:', error)
        } finally {
            setIsLoading(false)
        }
    }, [router])

    useEffect(() => {
        fetchInsightsData()
    }, [fetchInsightsData])

    const refreshInsights = async () => {
        setIsRefreshing(true)
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) return

            // Fetch fresh insights from Threads API
            await fetch(`${API_BASE_URL}/api/insights/refresh/${userId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `accessToken=${token}`
            })

            // Refresh the dashboard data
            fetchInsightsData()
        } catch (error) {
            console.error('Error refreshing insights:', error)
        } finally {
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

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        })
    }

    if (isLoading) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="container mx-auto px-4 py-8">
                    <div className="animate-pulse space-y-4">
                        <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                            {[...Array(4)].map((_, i) => (
                                <div key={i} className="h-32 bg-gray-200 rounded"></div>
                            ))}
                        </div>
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                            {[...Array(6)].map((_, i) => (
                                <div key={i} className="h-64 bg-gray-200 rounded"></div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    if (!dashboard) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="container mx-auto px-4 py-8">
                    <div className="text-center py-12">
                        <h2 className="text-2xl font-bold text-gray-900 mb-4">No Insights Available</h2>
                        <p className="text-gray-600 mb-6">We couldn&apos;t load your insights data. Please try refreshing.</p>
                        <Button onClick={refreshInsights} disabled={isRefreshing}>
                            <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
                            {isRefreshing ? 'Refreshing...' : 'Refresh Data'}
                        </Button>
                    </div>
                </div>
            </div>
        )
    }

    // Prepare data for charts
    const totalEngagementData = [
        { name: 'views', value: dashboard.totalMediaMetrics.total_views, color: COLORS[0] },
        { name: 'likes', value: dashboard.totalMediaMetrics.total_likes, color: COLORS[1] },
        { name: 'replies', value: dashboard.totalMediaMetrics.total_replies, color: COLORS[2] },
        { name: 'reposts', value: dashboard.totalMediaMetrics.total_reposts, color: COLORS[3] },
        { name: 'quotes', value: dashboard.totalMediaMetrics.total_quotes, color: COLORS[4] },
    ]

    const averageEngagementData = [
        { metric: 'Views', value: Math.round(dashboard.averageMetrics.avg_views) },
        { metric: 'Likes', value: Math.round(dashboard.averageMetrics.avg_likes) },
        { metric: 'Replies', value: Math.round(dashboard.averageMetrics.avg_replies) },
        { metric: 'Reposts', value: Math.round(dashboard.averageMetrics.avg_reposts) },
        { metric: 'Quotes', value: Math.round(dashboard.averageMetrics.avg_quotes) },
    ]

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Insights Dashboard</h1>
                        <p className="text-gray-600 mt-2">Comprehensive analytics for your Threads performance</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <Button onClick={refreshInsights} disabled={isRefreshing} variant="outline">
                            <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
                            {isRefreshing ? 'Refreshing...' : 'Refresh Data'}
                        </Button>
                    </div>
                </div>

                {/* Key Metrics Overview */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Views</CardTitle>
                            <Eye className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">
                                {formatNumber(dashboard.totalMediaMetrics.total_views)}
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Likes</CardTitle>
                            <Heart className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">
                                {formatNumber(dashboard.totalMediaMetrics.total_likes)}
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Replies</CardTitle>
                            <MessageCircle className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">
                                {formatNumber(dashboard.totalMediaMetrics.total_replies)}
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Posts</CardTitle>
                            <BarChart3 className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">
                                {formatNumber(dashboard.totalPosts)}
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* Charts Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    {/* Total Engagement Pie Chart */}
                    <Card>
                        <CardHeader>
                            <CardTitle>Total Engagement Distribution</CardTitle>
                            <CardDescription>
                                Breakdown of total engagement across all posts
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <ChartContainer config={chartConfig} className="mx-auto aspect-square max-h-[300px]">
                                <PieChart>
                                    <ChartTooltip
                                        cursor={false}
                                        content={<ChartTooltipContent hideLabel />}
                                    />
                                    <Pie
                                        data={totalEngagementData}
                                        dataKey="value"
                                        nameKey="name"
                                        innerRadius={60}
                                        strokeWidth={5}
                                    >
                                        {totalEngagementData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <ChartLegend content={<ChartLegendContent />} />
                                </PieChart>
                            </ChartContainer>
                        </CardContent>
                    </Card>

                    {/* Average Engagement Bar Chart */}
                    <Card>
                        <CardHeader>
                            <CardTitle>Average Engagement per Post</CardTitle>
                            <CardDescription>
                                Average metrics across all your posts
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <ChartContainer config={chartConfig} className="min-h-[200px]">
                                <BarChart data={averageEngagementData}>
                                    <CartesianGrid vertical={false} />
                                    <XAxis
                                        dataKey="metric"
                                        tickLine={false}
                                        tickMargin={10}
                                        axisLine={false}
                                    />
                                    <YAxis />
                                    <ChartTooltip
                                        cursor={false}
                                        content={<ChartTooltipContent hideLabel />}
                                    />
                                    <Bar dataKey="value" fill="#8884d8" radius={8} />
                                </BarChart>
                            </ChartContainer>
                        </CardContent>
                    </Card>

                    {/* Engagement Trends Area Chart */}
                    {dashboard.engagementTrends && dashboard.engagementTrends.length > 0 && (
                        <Card className="lg:col-span-2">
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <TrendingUp className="h-5 w-5 mr-2" />
                                    Engagement Trends Over Time
                                </CardTitle>
                                <CardDescription>
                                    Track your engagement metrics over time
                                </CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ChartContainer config={chartConfig} className="min-h-[200px]">
                                    <AreaChart data={dashboard.engagementTrends}>
                                        <CartesianGrid strokeDasharray="3 3" />
                                        <XAxis
                                            dataKey="date"
                                            tickFormatter={formatDate}
                                            tickLine={false}
                                            axisLine={false}
                                        />
                                        <YAxis />
                                        <ChartTooltip content={<ChartTooltipContent />} />
                                        <Area
                                            type="monotone"
                                            dataKey="views"
                                            stackId="1"
                                            stroke="#2563eb"
                                            fill="#2563eb"
                                            fillOpacity={0.6}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="likes"
                                            stackId="1"
                                            stroke="#dc2626"
                                            fill="#dc2626"
                                            fillOpacity={0.6}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="replies"
                                            stackId="1"
                                            stroke="#16a34a"
                                            fill="#16a34a"
                                            fillOpacity={0.6}
                                        />
                                    </AreaChart>
                                </ChartContainer>
                            </CardContent>
                        </Card>
                    )}

                    {/* Top Posts */}
                    {dashboard.topPosts && dashboard.topPosts.length > 0 && (
                        <Card className="lg:col-span-2">
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <Activity className="h-5 w-5 mr-2" />
                                    Top Performing Posts
                                </CardTitle>
                                <CardDescription>
                                    Your highest engagement posts
                                </CardDescription>
                            </CardHeader>
                            <CardContent>
                                <div className="space-y-4">
                                    {dashboard.topPosts.slice(0, 5).map((post, index) => (
                                        <div key={post.id} className="flex items-start space-x-4 p-4 border rounded-lg hover:bg-gray-50">
                                            <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                                                <span className="text-sm font-semibold text-blue-600">#{index + 1}</span>
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="text-sm text-gray-900 mb-2 leading-relaxed">
                                                    {post.text && post.text.length > 150 ? `${post.text.substring(0, 150)}...` : post.text || 'No content available'}
                                                </p>
                                                <div className="flex items-center space-x-4 text-xs text-gray-500">
                                                    <span className="flex items-center">
                                                        <Eye className="h-3 w-3 mr-1" />
                                                        {formatNumber(post.views || 0)} views
                                                    </span>
                                                    <span className="flex items-center">
                                                        <Heart className="h-3 w-3 mr-1" />
                                                        {formatNumber(post.likes || 0)} likes
                                                    </span>
                                                    <span className="flex items-center">
                                                        <MessageCircle className="h-3 w-3 mr-1" />
                                                        {formatNumber(post.replies || 0)} replies
                                                    </span>
                                                    <span className="text-gray-400">•</span>
                                                    <span>{formatDate(post.timestamp)}</span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </CardContent>
                        </Card>
                    )}

                    {/* User Metrics Summary */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center">
                                <Users className="h-5 w-5 mr-2" />
                                Account Metrics
                            </CardTitle>
                            <CardDescription>
                                Your overall account performance
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            {dashboard.userMetrics.followers_count !== undefined && (
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-medium">Followers</span>
                                    <span className="text-lg font-semibold">
                                        {formatNumber(dashboard.userMetrics.followers_count)}
                                    </span>
                                </div>
                            )}
                            {dashboard.userMetrics.views !== undefined && (
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-medium">Profile Views</span>
                                    <span className="text-lg font-semibold">
                                        {formatNumber(dashboard.userMetrics.views)}
                                    </span>
                                </div>
                            )}
                            {dashboard.userMetrics.clicks !== undefined && (
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-medium">Profile Clicks</span>
                                    <span className="text-lg font-semibold">
                                        {formatNumber(dashboard.userMetrics.clicks)}
                                    </span>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    )
} 