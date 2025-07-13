"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
    Search,
    Clock,
    TrendingUp,
    ExternalLink,
    RefreshCw,
    Database,
    BarChart3,
    History
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface SearchResult {
    id: number
    query: string
    searchType: string
    postId: string
    text: string
    mediaType: string
    permalink: string
    timestamp: string
    username: string
    hasReplies: boolean
    isQuotePost: boolean
    isReply: boolean
    searchTimestamp: string
}

interface PopularQuery {
    query: string
    count: number
}

interface SearchAnalytics {
    totalSearches: number
    uniqueQueries: number
    avgResultsPerSearch: number
    startDate: string
    endDate: string
}

interface RecentKeyword {
    query: string
    timestamp: string
}

export default function SearchPage() {
    const [searchQuery, setSearchQuery] = useState('')
    const [searchType, setSearchType] = useState('TOP')
    const [searchResults, setSearchResults] = useState<SearchResult[]>([])
    const [searchHistory, setSearchHistory] = useState<string[]>([])
    const [popularQueries, setPopularQueries] = useState<PopularQuery[]>([])
    const [recentKeywords, setRecentKeywords] = useState<RecentKeyword[]>([])
    const [analytics, setAnalytics] = useState<SearchAnalytics | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [useCache, setUseCache] = useState(true)
    const router = useRouter()

    useEffect(() => {
        fetchSearchHistory()
        fetchPopularQueries()
        fetchRecentKeywords()
        fetchAnalytics()
    }, [])

    const performSearch = async () => {
        if (!searchQuery.trim()) return

        setIsLoading(true)
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            const endpoint = useCache ? 'posts' : 'fresh'
            const url = `${API_BASE_URL}/api/search/${endpoint}?query=${encodeURIComponent(searchQuery)}&searchType=${searchType}&userId=${userId}&accessToken=${token}&useCache=${useCache}`

            const response = await fetch(url)
            if (response.ok) {
                const results = await response.json()
                setSearchResults(results)

                // Refresh search history and analytics
                fetchSearchHistory()
                fetchAnalytics()
            }
        } catch (error) {
            console.error('Error performing search:', error)
        } finally {
            setIsLoading(false)
        }
    }

    const fetchSearchHistory = async () => {
        try {
            const userId = localStorage.getItem('threads_user_id')
            if (!userId) return

            const response = await fetch(`${API_BASE_URL}/api/search/history/${userId}?limit=10`)
            if (response.ok) {
                const history = await response.json()
                setSearchHistory(history)
            }
        } catch (error) {
            console.error('Error fetching search history:', error)
        }
    }

    const fetchPopularQueries = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/search/popular?limit=10`)
            if (response.ok) {
                const queries = await response.json()
                setPopularQueries(queries)
            }
        } catch (error) {
            console.error('Error fetching popular queries:', error)
        }
    }

    const fetchRecentKeywords = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            if (!token) return

            const response = await fetch(`${API_BASE_URL}/api/search/recent-keywords?accessToken=${token}`)
            if (response.ok) {
                const keywords = await response.json()
                setRecentKeywords(keywords)
            }
        } catch (error) {
            console.error('Error fetching recent keywords:', error)
        }
    }

    const fetchAnalytics = async () => {
        try {
            const userId = localStorage.getItem('threads_user_id')
            if (!userId) return

            const endDate = new Date()
            const startDate = new Date()
            startDate.setDate(startDate.getDate() - 30)

            const url = `${API_BASE_URL}/api/search/analytics?userId=${userId}&startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`
            const response = await fetch(url)
            if (response.ok) {
                const analyticsData = await response.json()
                setAnalytics(analyticsData)
            }
        } catch (error) {
            console.error('Error fetching analytics:', error)
        }
    }

    const handleQuickSearch = (query: string) => {
        setSearchQuery(query)
        setSearchResults([])
    }

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    const getMediaTypeBadgeColor = (mediaType: string) => {
        switch (mediaType) {
            case 'TEXT_POST': return 'default'
            case 'IMAGE': return 'secondary'
            case 'VIDEO': return 'destructive'
            case 'CAROUSEL_ALBUM': return 'outline'
            default: return 'default'
        }
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Search Threads</h1>
                    <p className="text-gray-600">Search for posts across Threads with advanced caching and analytics</p>
                </div>

                {/* Search Interface */}
                <Card className="mb-8">
                    <CardHeader>
                        <CardTitle className="flex items-center">
                            <Search className="h-5 w-5 mr-2" />
                            Search Posts
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <div className="flex gap-4">
                                <div className="flex-1">
                                    <Input
                                        placeholder="Enter keywords to search..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && performSearch()}
                                    />
                                </div>
                                <Select value={searchType} onValueChange={setSearchType}>
                                    <SelectTrigger className="w-32">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="TOP">Top</SelectItem>
                                        <SelectItem value="RECENT">Recent</SelectItem>
                                    </SelectContent>
                                </Select>
                                <Button onClick={performSearch} disabled={isLoading || !searchQuery.trim()}>
                                    {isLoading ? (
                                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                                    ) : (
                                        <Search className="h-4 w-4 mr-2" />
                                    )}
                                    Search
                                </Button>
                            </div>

                            <div className="flex items-center gap-4">
                                <label className="flex items-center space-x-2">
                                    <input
                                        type="checkbox"
                                        checked={useCache}
                                        onChange={(e) => setUseCache(e.target.checked)}
                                        className="rounded"
                                    />
                                    <span className="text-sm">Use cached results (faster)</span>
                                </label>
                                {useCache && (
                                    <Badge variant="outline" className="text-xs">
                                        <Database className="h-3 w-3 mr-1" />
                                        1-hour cache
                                    </Badge>
                                )}
                            </div>
                        </div>
                    </CardContent>
                </Card>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Main Search Results */}
                    <div className="lg:col-span-2">
                        <Card>
                            <CardHeader>
                                <CardTitle>
                                    Search Results
                                    {searchResults.length > 0 && (
                                        <Badge variant="secondary" className="ml-2">
                                            {searchResults.length} results
                                        </Badge>
                                    )}
                                </CardTitle>
                                {searchQuery && (
                                    <CardDescription>
                                        Results for "{searchQuery}" ({searchType.toLowerCase()})
                                    </CardDescription>
                                )}
                            </CardHeader>
                            <CardContent>
                                {searchResults.length > 0 ? (
                                    <div className="rounded-md border">
                                        <Table>
                                            <TableHeader>
                                                <TableRow>
                                                    <TableHead>Content</TableHead>
                                                    <TableHead>Author</TableHead>
                                                    <TableHead>Type</TableHead>
                                                    <TableHead>Date</TableHead>
                                                    <TableHead>Actions</TableHead>
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {searchResults.map((result) => (
                                                    <TableRow key={result.id}>
                                                        <TableCell className="max-w-[300px]">
                                                            <div>
                                                                <p className="font-medium text-sm">
                                                                    {result.text.length > 80 ? `${result.text.substring(0, 80)}...` : result.text}
                                                                </p>
                                                                <div className="flex gap-1 mt-1">
                                                                    {result.hasReplies && (
                                                                        <Badge variant="outline" className="text-xs">
                                                                            Has Replies
                                                                        </Badge>
                                                                    )}
                                                                    {result.isQuotePost && (
                                                                        <Badge variant="secondary" className="text-xs">
                                                                            Quote
                                                                        </Badge>
                                                                    )}
                                                                    {result.isReply && (
                                                                        <Badge variant="outline" className="text-xs">
                                                                            Reply
                                                                        </Badge>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        </TableCell>
                                                        <TableCell>
                                                            <span className="text-sm font-medium">@{result.username}</span>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Badge variant={getMediaTypeBadgeColor(result.mediaType)}>
                                                                {result.mediaType}
                                                            </Badge>
                                                        </TableCell>
                                                        <TableCell>
                                                            <span className="text-sm text-gray-600">
                                                                {formatDate(result.timestamp)}
                                                            </span>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Button
                                                                size="sm"
                                                                variant="outline"
                                                                onClick={() => window.open(result.permalink, '_blank')}
                                                            >
                                                                <ExternalLink className="h-3 w-3 mr-1" />
                                                                View
                                                            </Button>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>
                                ) : searchQuery ? (
                                    <div className="text-center py-8 text-gray-500">
                                        {isLoading ? 'Searching...' : 'No results found. Try different keywords.'}
                                    </div>
                                ) : (
                                    <div className="text-center py-8 text-gray-500">
                                        Enter keywords above to start searching
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </div>

                    {/* Sidebar */}
                    <div className="space-y-6">
                        {/* Analytics Card */}
                        {analytics && (
                            <Card>
                                <CardHeader>
                                    <CardTitle className="flex items-center text-sm">
                                        <BarChart3 className="h-4 w-4 mr-2" />
                                        Search Analytics (30 days)
                                    </CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-3">
                                    <div className="flex justify-between">
                                        <span className="text-sm text-gray-600">Total Searches</span>
                                        <span className="font-medium">{analytics.totalSearches}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-sm text-gray-600">Unique Queries</span>
                                        <span className="font-medium">{analytics.uniqueQueries}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-sm text-gray-600">Avg Results</span>
                                        <span className="font-medium">{analytics.avgResultsPerSearch.toFixed(1)}</span>
                                    </div>
                                </CardContent>
                            </Card>
                        )}

                        {/* Search History */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center text-sm">
                                    <History className="h-4 w-4 mr-2" />
                                    Recent Searches
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                {searchHistory.length > 0 ? (
                                    <div className="space-y-2">
                                        {searchHistory.map((query, index) => (
                                            <button
                                                key={index}
                                                onClick={() => handleQuickSearch(query)}
                                                className="w-full text-left p-2 rounded hover:bg-gray-100 text-sm"
                                            >
                                                <Clock className="h-3 w-3 inline mr-2 text-gray-400" />
                                                {query}
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-sm text-gray-500">No recent searches</p>
                                )}
                            </CardContent>
                        </Card>

                        {/* Popular Queries */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center text-sm">
                                    <TrendingUp className="h-4 w-4 mr-2" />
                                    Popular Searches
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                {popularQueries.length > 0 ? (
                                    <div className="space-y-2">
                                        {popularQueries.map((item, index) => (
                                            <button
                                                key={index}
                                                onClick={() => handleQuickSearch(item.query)}
                                                className="w-full text-left p-2 rounded hover:bg-gray-100 text-sm flex justify-between items-center"
                                            >
                                                <span>{item.query}</span>
                                                <Badge variant="outline" className="text-xs">
                                                    {item.count}
                                                </Badge>
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-sm text-gray-500">No popular queries yet</p>
                                )}
                            </CardContent>
                        </Card>

                        {/* Recent Keywords from API */}
                        {recentKeywords.length > 0 && (
                            <Card>
                                <CardHeader>
                                    <CardTitle className="flex items-center text-sm">
                                        <Search className="h-4 w-4 mr-2" />
                                        Your Recent Keywords
                                    </CardTitle>
                                    <CardDescription className="text-xs">
                                        From Threads API
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="space-y-2">
                                        {recentKeywords.map((keyword, index) => (
                                            <button
                                                key={index}
                                                onClick={() => handleQuickSearch(keyword.query)}
                                                className="w-full text-left p-2 rounded hover:bg-gray-100 text-sm"
                                            >
                                                <div className="flex justify-between items-center">
                                                    <span>{keyword.query}</span>
                                                    <span className="text-xs text-gray-400">
                                                        {formatDate(keyword.timestamp)}
                                                    </span>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                </CardContent>
                            </Card>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
} 