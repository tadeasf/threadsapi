"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    ChartContainer,
    ChartTooltip,
    ChartTooltipContent,
    ChartLegend,
    ChartLegendContent,
} from "@/components/ui/chart"
import { Bar, BarChart, Line, LineChart, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from 'recharts'
import {
    TrendingUp,
    TrendingDown,
    Eye,
    Heart,
    MessageCircle,
    Repeat,
    Calendar,
    RefreshCw,
    BarChart3,
    Activity
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface UserInsightsDashboard {
    currentMetrics: {
        views?: number
        followers?: number
        follower_demographics?: number
    }
    dailyViews: Array<{
        date: string
        value: number
    }>
}

interface PostPerformanceAnalytics {
    metricSummaries: {
        views?: MetricSummary
        likes?: MetricSummary
        replies?: MetricSummary
        reposts?: MetricSummary
        quotes?: MetricSummary
    }
    startDate: string
    endDate: string
}

interface MetricSummary {
    total: number
    average: number
    max: number
    min: number
    count: number
}

interface EngagementTrend {
    date: string
    views: number
    likes: number
    replies: number
    reposts: number
    quotes: number
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

export default function InsightsPage() {
    const [dashboard, setDashboard] = useState<UserInsightsDashboard | null>(null)
    const [performance, setPerformance] = useState<PostPerformanceAnalytics | null>(null)
    const [trends, setTrends] = useState<EngagementTrend[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [selectedPeriod, setSelectedPeriod] = useState('30')
    const router = useRouter()

    useEffect(() => {
        fetchInsightsData()
    }, [selectedPeriod])

    const fetchInsightsData = async () => {
        setIsLoading(true)
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            // Fetch all insights data in parallel
            const [dashboardRes, performanceRes, trendsRes] = await Promise.all([
                fetch(`${API_BASE_URL}/api/insights/user/${userId}/dashboard?accessToken=${token}`),
                fetch(`${API_BASE_URL}/api/insights/performance/${userId}?days=${selectedPeriod}&accessToken=${token}`),
                fetch(`${API_BASE_URL}/api/insights/trends/${userId}?days=${selectedPeriod}&accessToken=${token}`)
            ])

            if (dashboardRes.ok) {
                const dashboardData = await dashboardRes.json()
                setDashboard(dashboardData)
            }

            if (performanceRes.ok) {
                const performanceData = await performanceRes.json()
                setPerformance(performanceData)
            }

            if (trendsRes.ok) {
                const trendsData = await trendsRes.json()
                setTrends(trendsData)
            }

        } catch (error) {
            console.error('Error fetching insights data:', error)
        } finally {
            setIsLoading(false)
        }
    }

    const refreshInsights = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) return

            // Fetch fresh insights from Threads API
            await fetch(`${API_BASE_URL}/api/insights/user/${userId}?accessToken=${token}`)

            // Refresh the dashboard data
            fetchInsightsData()
        } catch (error) {
            console.error('Error refreshing insights:', error)
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

    const calculatePercentageChange = (current: number, previous: number) => {
        if (previous === 0) return 0
        return ((current - previous) / previous) * 100
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

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Insights Dashboard</h1>
                        <p className="text-gray-600 mt-2">Analyze your Threads performance and engagement</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <Select value={selectedPeriod} onValueChange={setSelectedPeriod}>
                            <SelectTrigger className="w-32">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="7">Last 7 days</SelectItem>
                                <SelectItem value="30">Last 30 days</SelectItem>
                                <SelectItem value="90">Last 90 days</SelectItem>
                            </SelectContent>
                        </Select>
                        <Button onClick={refreshInsights} variant="outline">
                            <RefreshCw className="h-4 w-4 mr-2" />
                            Refresh Data
                        </Button>
                    </div>
                </div>

                {/* Current Metrics Overview */}
                {dashboard && (
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Views</CardTitle>
                                <Eye className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">
                                    {formatNumber(dashboard.currentMetrics.views || 0)}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    Lifetime views
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Followers</CardTitle>
                                <Activity className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">
                                    {formatNumber(dashboard.currentMetrics.followers || 0)}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    Total followers
                                </p>
                            </CardContent>
                        </Card>

                        {performance && (
                            <>
                                <Card>
                                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                        <CardTitle className="text-sm font-medium">Avg. Engagement</CardTitle>
                                        <TrendingUp className="h-4 w-4 text-muted-foreground" />
                                    </CardHeader>
                                    <CardContent>
                                        <div className="text-2xl font-bold">
                                            {performance.metricSummaries.likes ?
                                                formatNumber(Math.round(performance.metricSummaries.likes.average)) : '0'
                                            }
                                        </div>
                                        <p className="text-xs text-muted-foreground">
                                            Likes per post
                                        </p>
                                    </CardContent>
                                </Card>

                                <Card>
                                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                        <CardTitle className="text-sm font-medium">Posts Analyzed</CardTitle>
                                        <BarChart3 className="h-4 w-4 text-muted-foreground" />
                                    </CardHeader>
                                    <CardContent>
                                        <div className="text-2xl font-bold">
                                            {performance.metricSummaries.views?.count || 0}
                                        </div>
                                        <p className="text-xs text-muted-foreground">
                                            In selected period
                                        </p>
                                    </CardContent>
                                </Card>
                            </>
                        )}
                    </div>
                )}

                <Tabs defaultValue="overview" className="space-y-6">
                    <TabsList className="grid w-full grid-cols-3">
                        <TabsTrigger value="overview">Overview</TabsTrigger>
                        <TabsTrigger value="performance">Performance</TabsTrigger>
                        <TabsTrigger value="trends">Trends</TabsTrigger>
                    </TabsList>

                    {/* Overview Tab */}
                    <TabsContent value="overview" className="space-y-6">
                        {/* Daily Views Chart */}
                        {dashboard && dashboard.dailyViews.length > 0 && (
                            <Card>
                                <CardHeader>
                                    <CardTitle>Daily Views</CardTitle>
                                    <CardDescription>
                                        View count over the last {dashboard.dailyViews.length} days
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <ChartContainer config={chartConfig} className="h-[300px] w-full">
                                        <LineChart data={dashboard.dailyViews}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis
                                                dataKey="date"
                                                tickFormatter={(value) => formatDate(value)}
                                                fontSize={12}
                                            />
                                            <YAxis fontSize={12} />
                                            <ChartTooltip content={<ChartTooltipContent />} />
                                            <Line
                                                type="monotone"
                                                dataKey="value"
                                                stroke="var(--color-views)"
                                                strokeWidth={2}
                                                dot={{ fill: "var(--color-views)" }}
                                            />
                                        </LineChart>
                                    </ChartContainer>
                                </CardContent>
                            </Card>
                        )}

                        {/* Performance Summary */}
                        {performance && (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {Object.entries(performance.metricSummaries).map(([metric, summary]) => (
                                    <Card key={metric}>
                                        <CardHeader className="pb-3">
                                            <CardTitle className="text-lg capitalize flex items-center">
                                                {metric === 'views' && <Eye className="h-4 w-4 mr-2" />}
                                                {metric === 'likes' && <Heart className="h-4 w-4 mr-2" />}
                                                {metric === 'replies' && <MessageCircle className="h-4 w-4 mr-2" />}
                                                {metric === 'reposts' && <Repeat className="h-4 w-4 mr-2" />}
                                                {metric === 'quotes' && <BarChart3 className="h-4 w-4 mr-2" />}
                                                {metric}
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent className="space-y-3">
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-600">Total</span>
                                                <span className="font-semibold">{formatNumber(summary.total)}</span>
                                            </div>
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-600">Average</span>
                                                <span className="font-semibold">{formatNumber(Math.round(summary.average))}</span>
                                            </div>
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-600">Best Post</span>
                                                <span className="font-semibold">{formatNumber(summary.max)}</span>
                                            </div>
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-600">Posts</span>
                                                <span className="font-semibold">{summary.count}</span>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        )}
                    </TabsContent>

                    {/* Performance Tab */}
                    <TabsContent value="performance" className="space-y-6">
                        {performance && (
                            <Card>
                                <CardHeader>
                                    <CardTitle>Engagement Metrics Comparison</CardTitle>
                                    <CardDescription>
                                        Average performance across different engagement types
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <ChartContainer config={chartConfig} className="h-[400px] w-full">
                                        <BarChart
                                            data={[
                                                {
                                                    metric: 'Views',
                                                    value: performance.metricSummaries.views?.average || 0,
                                                    total: performance.metricSummaries.views?.total || 0
                                                },
                                                {
                                                    metric: 'Likes',
                                                    value: performance.metricSummaries.likes?.average || 0,
                                                    total: performance.metricSummaries.likes?.total || 0
                                                },
                                                {
                                                    metric: 'Replies',
                                                    value: performance.metricSummaries.replies?.average || 0,
                                                    total: performance.metricSummaries.replies?.total || 0
                                                },
                                                {
                                                    metric: 'Reposts',
                                                    value: performance.metricSummaries.reposts?.average || 0,
                                                    total: performance.metricSummaries.reposts?.total || 0
                                                },
                                                {
                                                    metric: 'Quotes',
                                                    value: performance.metricSummaries.quotes?.average || 0,
                                                    total: performance.metricSummaries.quotes?.total || 0
                                                }
                                            ]}
                                        >
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis dataKey="metric" fontSize={12} />
                                            <YAxis fontSize={12} />
                                            <ChartTooltip
                                                content={({ active, payload, label }) => {
                                                    if (active && payload && payload.length) {
                                                        const data = payload[0].payload
                                                        return (
                                                            <div className="bg-white p-3 border rounded shadow-lg">
                                                                <p className="font-semibold">{label}</p>
                                                                <p className="text-sm">Average: {formatNumber(Math.round(data.value))}</p>
                                                                <p className="text-sm">Total: {formatNumber(data.total)}</p>
                                                            </div>
                                                        )
                                                    }
                                                    return null
                                                }}
                                            />
                                            <Bar dataKey="value" fill="#2563eb" radius={4} />
                                        </BarChart>
                                    </ChartContainer>
                                </CardContent>
                            </Card>
                        )}
                    </TabsContent>

                    {/* Trends Tab */}
                    <TabsContent value="trends" className="space-y-6">
                        {trends.length > 0 && (
                            <Card>
                                <CardHeader>
                                    <CardTitle>Engagement Trends Over Time</CardTitle>
                                    <CardDescription>
                                        Track how your engagement metrics change over time
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <ChartContainer config={chartConfig} className="h-[400px] w-full">
                                        <LineChart data={trends}>
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis
                                                dataKey="date"
                                                tickFormatter={(value) => formatDate(value)}
                                                fontSize={12}
                                            />
                                            <YAxis fontSize={12} />
                                            <ChartTooltip content={<ChartTooltipContent />} />
                                            <ChartLegend content={<ChartLegendContent />} />
                                            <Line
                                                type="monotone"
                                                dataKey="views"
                                                stroke="var(--color-views)"
                                                strokeWidth={2}
                                                name="Views"
                                            />
                                            <Line
                                                type="monotone"
                                                dataKey="likes"
                                                stroke="var(--color-likes)"
                                                strokeWidth={2}
                                                name="Likes"
                                            />
                                            <Line
                                                type="monotone"
                                                dataKey="replies"
                                                stroke="var(--color-replies)"
                                                strokeWidth={2}
                                                name="Replies"
                                            />
                                            <Line
                                                type="monotone"
                                                dataKey="reposts"
                                                stroke="var(--color-reposts)"
                                                strokeWidth={2}
                                                name="Reposts"
                                            />
                                            <Line
                                                type="monotone"
                                                dataKey="quotes"
                                                stroke="var(--color-quotes)"
                                                strokeWidth={2}
                                                name="Quotes"
                                            />
                                        </LineChart>
                                    </ChartContainer>
                                </CardContent>
                            </Card>
                        )}
                    </TabsContent>
                </Tabs>
            </div>
        </div>
    )
} 