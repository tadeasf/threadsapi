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
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

import {
    MoreHorizontal,
    Eye,
    Trash2,
    TrendingUp,
    Search,
    Filter,

    RefreshCw,
    Plus
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface ThreadsPost {
    id: string
    text: string | null
    mediaType: string
    mediaUrl?: string
    permalink: string
    timestamp: string
    viewsCount: number
    likesCount: number
    repliesCount: number
    repostsCount: number
    quotesCount: number
    hasReplies: boolean
    isQuotePost: boolean
    isReply: boolean
}

interface PostStatistics {
    totalPosts: number
    totalViews: number
    totalLikes: number
}

export default function PostsPage() {
    const [posts, setPosts] = useState<ThreadsPost[]>([])
    const [filteredPosts, setFilteredPosts] = useState<ThreadsPost[]>([])
    const [statistics, setStatistics] = useState<PostStatistics | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [searchTerm, setSearchTerm] = useState('')
    const [selectedMediaType, setSelectedMediaType] = useState<string>('all')
    const [sortBy, setSortBy] = useState<'timestamp' | 'views' | 'likes'>('timestamp')
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')
    const router = useRouter()

    useEffect(() => {
        fetchPosts()
        fetchStatistics()
    }, [])

    useEffect(() => {
        filterAndSortPosts()
    }, [posts, searchTerm, selectedMediaType, sortBy, sortOrder])

    const fetchPosts = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            const response = await fetch(`${API_BASE_URL}/api/posts/user/${userId}/insights?accessToken=${token}`)
            if (response.ok) {
                const data = await response.json()
                setPosts(data)
            }
        } catch (error) {
            console.error('Error fetching posts:', error)
        } finally {
            setIsLoading(false)
        }
    }

    const fetchStatistics = async () => {
        try {
            const userId = localStorage.getItem('threads_user_id')
            if (!userId) return

            const response = await fetch(`${API_BASE_URL}/api/posts/user/${userId}/statistics`)
            if (response.ok) {
                const data = await response.json()
                setStatistics(data)
            }
        } catch (error) {
            console.error('Error fetching statistics:', error)
        }
    }

    const filterAndSortPosts = () => {
        const filtered = posts.filter(post => {
            const postText = post.text || '' // Handle null/undefined text
            const matchesSearch = postText.toLowerCase().includes(searchTerm.toLowerCase())
            const matchesMediaType = selectedMediaType === 'all' || post.mediaType === selectedMediaType
            return matchesSearch && matchesMediaType
        })

        filtered.sort((a, b) => {
            let aValue: number | string
            let bValue: number | string

            switch (sortBy) {
                case 'views':
                    aValue = a.viewsCount
                    bValue = b.viewsCount
                    break
                case 'likes':
                    aValue = a.likesCount
                    bValue = b.likesCount
                    break
                default:
                    aValue = new Date(a.timestamp).getTime()
                    bValue = new Date(b.timestamp).getTime()
            }

            if (sortOrder === 'asc') {
                return aValue > bValue ? 1 : -1
            } else {
                return aValue < bValue ? 1 : -1
            }
        })

        setFilteredPosts(filtered)
    }

    const handleDeletePost = async (postId: string) => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const response = await fetch(`${API_BASE_URL}/api/posts/${postId}?accessToken=${token}`, {
                method: 'DELETE'
            })

            if (response.ok) {
                setPosts(posts.filter(post => post.id !== postId))
                fetchStatistics() // Refresh statistics
            }
        } catch (error) {
            console.error('Error deleting post:', error)
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
            year: 'numeric',
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

    const mediaTypes = ['all', 'TEXT_POST', 'IMAGE', 'VIDEO', 'CAROUSEL_ALBUM']

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
                        <h1 className="text-3xl font-bold text-gray-900">Posts Management</h1>
                        <p className="text-gray-600 mt-2">Manage your Threads posts and analyze performance</p>
                    </div>
                    <Button onClick={() => router.push('/create-post')}>
                        <Plus className="h-4 w-4 mr-2" />
                        Create New Post
                    </Button>
                </div>

                {/* Statistics Cards */}
                {statistics && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Posts</CardTitle>
                                <TrendingUp className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">{formatNumber(statistics.totalPosts)}</div>
                                <p className="text-xs text-muted-foreground">
                                    {filteredPosts.length !== statistics.totalPosts &&
                                        `${filteredPosts.length} filtered`
                                    }
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Views</CardTitle>
                                <Eye className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">{formatNumber(statistics.totalViews)}</div>
                                <p className="text-xs text-muted-foreground">
                                    Across all posts
                                </p>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Total Likes</CardTitle>
                                <TrendingUp className="h-4 w-4 text-muted-foreground" />
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">{formatNumber(statistics.totalLikes)}</div>
                                <p className="text-xs text-muted-foreground">
                                    Total engagement
                                </p>
                            </CardContent>
                        </Card>
                    </div>
                )}

                {/* Filters and Search */}
                <Card className="mb-6">
                    <CardHeader>
                        <CardTitle className="text-lg">Filters & Search</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="flex flex-col md:flex-row gap-4">
                            <div className="flex-1">
                                <div className="relative">
                                    <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                                    <Input
                                        placeholder="Search posts by content..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        className="pl-9"
                                    />
                                </div>
                            </div>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button variant="outline">
                                        <Filter className="h-4 w-4 mr-2" />
                                        Media Type: {selectedMediaType === 'all' ? 'All' : selectedMediaType}
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent>
                                    {mediaTypes.map(type => (
                                        <DropdownMenuItem
                                            key={type}
                                            onClick={() => setSelectedMediaType(type)}
                                        >
                                            {type === 'all' ? 'All Types' : type}
                                        </DropdownMenuItem>
                                    ))}
                                </DropdownMenuContent>
                            </DropdownMenu>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button variant="outline">
                                        Sort: {sortBy} ({sortOrder})
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent>
                                    <DropdownMenuLabel>Sort By</DropdownMenuLabel>
                                    <DropdownMenuItem onClick={() => setSortBy('timestamp')}>
                                        Date
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => setSortBy('views')}>
                                        Views
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => setSortBy('likes')}>
                                        Likes
                                    </DropdownMenuItem>
                                    <DropdownMenuSeparator />
                                    <DropdownMenuLabel>Order</DropdownMenuLabel>
                                    <DropdownMenuItem onClick={() => setSortOrder('desc')}>
                                        Descending
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => setSortOrder('asc')}>
                                        Ascending
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>

                            <Button variant="outline" onClick={fetchPosts}>
                                <RefreshCw className="h-4 w-4 mr-2" />
                                Refresh
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                {/* Posts Table */}
                <Card>
                    <CardHeader>
                        <CardTitle>Your Posts ({filteredPosts.length})</CardTitle>
                        <CardDescription>
                            Manage and analyze your Threads posts
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="rounded-md border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Content</TableHead>
                                        <TableHead>Type</TableHead>
                                        <TableHead>Date</TableHead>
                                        <TableHead className="text-right">Views</TableHead>
                                        <TableHead className="text-right">Likes</TableHead>
                                        <TableHead className="text-right">Replies</TableHead>
                                        <TableHead className="text-right">Engagement</TableHead>
                                        <TableHead className="w-[70px]">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {filteredPosts.length > 0 ? (
                                        filteredPosts.map((post) => (
                                            <TableRow key={post.id}>
                                                <TableCell className="max-w-[300px]">
                                                    <div>
                                                        <p className="font-medium truncate">
                                                            {post.text ? (post.text.length > 60 ? `${post.text.substring(0, 60)}...` : post.text) : 'No text content'}
                                                        </p>
                                                        {post.hasReplies && (
                                                            <Badge variant="outline" className="mt-1 text-xs">
                                                                Has Replies
                                                            </Badge>
                                                        )}
                                                        {post.isQuotePost && (
                                                            <Badge variant="secondary" className="mt-1 ml-1 text-xs">
                                                                Quote Post
                                                            </Badge>
                                                        )}
                                                    </div>
                                                </TableCell>
                                                <TableCell>
                                                    <Badge variant={getMediaTypeBadgeColor(post.mediaType)}>
                                                        {post.mediaType}
                                                    </Badge>
                                                </TableCell>
                                                <TableCell>
                                                    <span className="text-sm text-gray-600">
                                                        {formatDate(post.timestamp)}
                                                    </span>
                                                </TableCell>
                                                <TableCell className="text-right font-medium">
                                                    {formatNumber(post.viewsCount)}
                                                </TableCell>
                                                <TableCell className="text-right font-medium">
                                                    {formatNumber(post.likesCount)}
                                                </TableCell>
                                                <TableCell className="text-right font-medium">
                                                    {formatNumber(post.repliesCount)}
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    <span className="text-sm font-medium">
                                                        {formatNumber(post.likesCount + post.repliesCount + post.repostsCount)}
                                                    </span>
                                                </TableCell>
                                                <TableCell>
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button variant="ghost" className="h-8 w-8 p-0">
                                                                <MoreHorizontal className="h-4 w-4" />
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end">
                                                            <DropdownMenuItem
                                                                onClick={() => window.open(post.permalink, '_blank')}
                                                            >
                                                                <Eye className="mr-2 h-4 w-4" />
                                                                View on Threads
                                                            </DropdownMenuItem>
                                                            <DropdownMenuItem
                                                                onClick={() => router.push(`/post/${post.id}`)}
                                                            >
                                                                <TrendingUp className="mr-2 h-4 w-4" />
                                                                View Analytics
                                                            </DropdownMenuItem>
                                                            <DropdownMenuSeparator />
                                                            <DropdownMenuItem
                                                                onClick={() => handleDeletePost(post.id)}
                                                                className="text-red-600"
                                                            >
                                                                <Trash2 className="mr-2 h-4 w-4" />
                                                                Delete Post
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    ) : (
                                        <TableRow>
                                            <TableCell colSpan={8} className="h-24 text-center">
                                                {searchTerm || selectedMediaType !== 'all' ?
                                                    'No posts match your filters.' :
                                                    'No posts found. Create your first post!'
                                                }
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
} 