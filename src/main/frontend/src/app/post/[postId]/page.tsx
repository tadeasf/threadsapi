"use client"

import { useState, useEffect, useCallback } from 'react'
import { useRouter, useParams } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
} from "@/components/ui/chart"
import { Line, LineChart, XAxis, YAxis, CartesianGrid, Bar, BarChart } from 'recharts'
import {
    Eye,
    Heart,
    MessageCircle,
    RefreshCw,
    ArrowLeft,
    ExternalLink,
    Calendar,
    BarChart3,
    Repeat,
    Quote,
    Activity
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface PostDetailedInsights {
    postId: string
    text: string
    timestamp: string
    currentMetrics: {
        views: number
        likes: number
        replies: number
        reposts: number
        quotes: number
    }
    userAverages: {
        views: number
        likes: number
        replies: number
        reposts: number
        quotes: number
    }
    performanceVsAverage: {
        views: number
        likes: number
        replies: number
        reposts: number
        quotes: number
    }
    totalEngagement: number
    engagementRate: number
    postRank: number
    totalUserPosts: number
    historicalInsights: Array<{
        date: string
        views: number
        likes: number
        replies: number
        reposts: number
        quotes: number
    }>
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
}

export default function PostAnalysisPage() {
    const [insights, setInsights] = useState<PostDetailedInsights | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const router = useRouter()
    const params = useParams()
    const postId = params.postId as string

    const fetchPostInsights = useCallback(async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            const response = await fetch(`${API_BASE_URL}/api/insights/post/${postId}/detailed?userId=${userId}&accessToken=${token}`)

            if (response.ok) {
                const data = await response.json()
                setInsights(data)
            } else {
                const errorText = await response.text()
                setError(`Failed to fetch insights: ${errorText}`)
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch insights')
        } finally {
            setIsLoading(false)
            setIsRefreshing(false)
        }
    }, [postId, router])

    useEffect(() => {
        fetchPostInsights()
    }, [fetchPostInsights])

    const refreshInsights = async () => {
        setIsRefreshing(true)
        await fetchPostInsights()
        setIsRefreshing(false)
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
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    const formatPercentage = (value: number) => {
        const sign = value > 0 ? '+' : ''
        return `${sign}${(value * 100).toFixed(1)}%`
    }

    const getPerformanceColor = (value: number) => {
        if (value > 0.2) return 'text-green-600'
        if (value < -0.2) return 'text-red-600'
        return 'text-gray-600'
    }

    const getPerformanceBadge = (value: number) => {
        if (value > 0.5) return { variant: 'default' as const, text: 'Excellent' }
        if (value > 0.2) return { variant: 'secondary' as const, text: 'Above Average' }
        if (value > -0.2) return { variant: 'outline' as const, text: 'Average' }
        return { variant: 'destructive' as const, text: 'Below Average' }
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
                        <div className="h-64 bg-gray-200 rounded"></div>
                    </div>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gray-50">
                <Header />
                <div className="container mx-auto px-4 py-8">
                    <div className="text-center py-12">
                        <h2 className="text-2xl font-bold text-gray-900 mb-4">Error Loading Post</h2>
                        <p className="text-gray-600 mb-6">{error}</p>
                        <div className="space-x-4">
                            <Button onClick={() => router.back()}>
                                <ArrowLeft className="h-4 w-4 mr-2" />
                                Go Back
                            </Button>
                            <Button onClick={refreshInsights} variant="outline">
                                <RefreshCw className="h-4 w-4 mr-2" />
                                Try Again
                            </Button>
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    if (!insights) {
        return null
    }

    // Prepare chart data
    const comparisonData = [
        { metric: 'Views', current: insights.currentMetrics.views, average: insights.userAverages.views },
        { metric: 'Likes', current: insights.currentMetrics.likes, average: insights.userAverages.likes },
        { metric: 'Replies', current: insights.currentMetrics.replies, average: insights.userAverages.replies },
        { metric: 'Reposts', current: insights.currentMetrics.reposts, average: insights.userAverages.reposts },
        { metric: 'Quotes', current: insights.currentMetrics.quotes, average: insights.userAverages.quotes },
    ]

    const overallPerformance = getPerformanceBadge(
        (insights.performanceVsAverage.views + insights.performanceVsAverage.likes +
            insights.performanceVsAverage.replies + insights.performanceVsAverage.reposts +
            insights.performanceVsAverage.quotes) / 5
    )

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center space-x-4">
                        <Button onClick={() => router.back()} variant="outline">
                            <ArrowLeft className="h-4 w-4 mr-2" />
                            Back
                        </Button>
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Post Analytics</h1>
                            <p className="text-gray-600 mt-2">Detailed insights for post {postId}</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-4">
                        <Button onClick={refreshInsights} disabled={isRefreshing} variant="outline">
                            <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
                            {isRefreshing ? 'Refreshing...' : 'Refresh Data'}
                        </Button>
                        <Button asChild variant="outline">
                            <a href={`https://threads.net/t/${postId}`} target="_blank" rel="noopener noreferrer">
                                <ExternalLink className="h-4 w-4 mr-2" />
                                View on Threads
                            </a>
                        </Button>
                    </div>
                </div>

                {/* Post Content */}
                <Card className="mb-8">
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <CardTitle className="flex items-center">
                                <Activity className="h-5 w-5 mr-2" />
                                Post Content
                            </CardTitle>
                            <div className="flex items-center space-x-2">
                                <Badge variant={overallPerformance.variant}>
                                    {overallPerformance.text}
                                </Badge>
                                <Badge variant="outline">
                                    Rank #{insights.postRank} of {insights.totalUserPosts}
                                </Badge>
                            </div>
                        </div>
                        <CardDescription className="flex items-center space-x-4">
                            <span className="flex items-center">
                                <Calendar className="h-4 w-4 mr-1" />
                                {formatDate(insights.timestamp)}
                            </span>
                            <span>Engagement Rate: {(insights.engagementRate * 100).toFixed(1)}%</span>
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-gray-900 leading-relaxed">
                            {insights.text || 'No text content available'}
                        </p>
                    </CardContent>
                </Card>

                {/* Key Metrics */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Views</CardTitle>
                            <Eye className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{formatNumber(insights.currentMetrics.views)}</div>
                            <p className={`text-xs ${getPerformanceColor(insights.performanceVsAverage.views)}`}>
                                {formatPercentage(insights.performanceVsAverage.views)} vs avg
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Likes</CardTitle>
                            <Heart className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{formatNumber(insights.currentMetrics.likes)}</div>
                            <p className={`text-xs ${getPerformanceColor(insights.performanceVsAverage.likes)}`}>
                                {formatPercentage(insights.performanceVsAverage.likes)} vs avg
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Replies</CardTitle>
                            <MessageCircle className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{formatNumber(insights.currentMetrics.replies)}</div>
                            <p className={`text-xs ${getPerformanceColor(insights.performanceVsAverage.replies)}`}>
                                {formatPercentage(insights.performanceVsAverage.replies)} vs avg
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Reposts</CardTitle>
                            <Repeat className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{formatNumber(insights.currentMetrics.reposts)}</div>
                            <p className={`text-xs ${getPerformanceColor(insights.performanceVsAverage.reposts)}`}>
                                {formatPercentage(insights.performanceVsAverage.reposts)} vs avg
                            </p>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Quotes</CardTitle>
                            <Quote className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{formatNumber(insights.currentMetrics.quotes)}</div>
                            <p className={`text-xs ${getPerformanceColor(insights.performanceVsAverage.quotes)}`}>
                                {formatPercentage(insights.performanceVsAverage.quotes)} vs avg
                            </p>
                        </CardContent>
                    </Card>
                </div>

                {/* Charts */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    {/* Performance vs Average */}
                    <Card>
                        <CardHeader>
                            <CardTitle>Performance vs Your Average</CardTitle>
                            <CardDescription>
                                How this post compares to your typical performance
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <ChartContainer config={chartConfig} className="min-h-[200px]">
                                <BarChart data={comparisonData}>
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
                                        content={<ChartTooltipContent />}
                                    />
                                    <Bar dataKey="current" fill="#2563eb" radius={4} name="This Post" />
                                    <Bar dataKey="average" fill="#e5e7eb" radius={4} name="Your Average" />
                                </BarChart>
                            </ChartContainer>
                        </CardContent>
                    </Card>

                    {/* Historical Trends */}
                    {insights.historicalInsights && insights.historicalInsights.length > 0 && (
                        <Card>
                            <CardHeader>
                                <CardTitle>Historical Performance</CardTitle>
                                <CardDescription>
                                    Engagement trends over time
                                </CardDescription>
                            </CardHeader>
                            <CardContent>
                                <ChartContainer config={chartConfig} className="min-h-[200px]">
                                    <LineChart data={insights.historicalInsights}>
                                        <CartesianGrid strokeDasharray="3 3" />
                                        <XAxis
                                            dataKey="date"
                                            tickFormatter={(value) => new Date(value).toLocaleDateString()}
                                            tickLine={false}
                                            axisLine={false}
                                        />
                                        <YAxis />
                                        <ChartTooltip content={<ChartTooltipContent />} />
                                        <Line
                                            type="monotone"
                                            dataKey="views"
                                            stroke="#2563eb"
                                            strokeWidth={2}
                                            dot={{ fill: '#2563eb' }}
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="likes"
                                            stroke="#dc2626"
                                            strokeWidth={2}
                                            dot={{ fill: '#dc2626' }}
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="replies"
                                            stroke="#16a34a"
                                            strokeWidth={2}
                                            dot={{ fill: '#16a34a' }}
                                        />
                                    </LineChart>
                                </ChartContainer>
                            </CardContent>
                        </Card>
                    )}
                </div>

                {/* Summary Stats */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center">
                            <BarChart3 className="h-5 w-5 mr-2" />
                            Summary Statistics
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <div className="text-center">
                                <div className="text-2xl font-bold text-blue-600">
                                    {formatNumber(insights.totalEngagement)}
                                </div>
                                <p className="text-sm text-gray-600">Total Engagement</p>
                            </div>
                            <div className="text-center">
                                <div className="text-2xl font-bold text-green-600">
                                    {(insights.engagementRate * 100).toFixed(1)}%
                                </div>
                                <p className="text-sm text-gray-600">Engagement Rate</p>
                            </div>
                            <div className="text-center">
                                <div className="text-2xl font-bold text-purple-600">
                                    #{insights.postRank}
                                </div>
                                <p className="text-sm text-gray-600">Rank out of {insights.totalUserPosts} posts</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
} 