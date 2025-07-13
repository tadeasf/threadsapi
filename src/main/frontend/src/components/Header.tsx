"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from "@/components/ui/button"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import {
    User,
    Settings,
    LogOut,
    FileText,
    BarChart3,
    Search,
    Plus,
    Eye,
    Trash2,
    TrendingUp
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface UserProfile {
    id: string
    username: string
    name: string
    threads_profile_picture_url?: string
    threads_biography?: string
}

interface PostSummary {
    id: string
    text: string
    timestamp: string
    viewsCount: number
    likesCount: number
    mediaType: string
}

interface PostStatistics {
    totalPosts: number
    totalViews: number
    totalLikes: number
}

export default function Header() {
    const [userProfile, setUserProfile] = useState<UserProfile | null>(null)
    const [recentPosts, setRecentPosts] = useState<PostSummary[]>([])
    const [postStats, setPostStats] = useState<PostStatistics | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const router = useRouter()

    useEffect(() => {
        fetchUserData()
    }, [])

    const fetchUserData = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) {
                router.push('/login')
                return
            }

            // Fetch user profile
            const profileResponse = await fetch(`${API_BASE_URL}/api/user/profile?accessToken=${token}`)
            if (profileResponse.ok) {
                const profile = await profileResponse.json()
                setUserProfile(profile)
            }

            // Fetch recent posts
            const postsResponse = await fetch(`${API_BASE_URL}/api/posts/user/${userId}/paginated?page=0&size=5`)
            if (postsResponse.ok) {
                const postsData = await postsResponse.json()
                setRecentPosts(postsData.content || [])
            }

            // Fetch post statistics
            const statsResponse = await fetch(`${API_BASE_URL}/api/posts/user/${userId}/statistics`)
            if (statsResponse.ok) {
                const stats = await statsResponse.json()
                setPostStats(stats)
            }

        } catch (error) {
            console.error('Error fetching user data:', error)
        } finally {
            setIsLoading(false)
        }
    }

    const handleLogout = () => {
        localStorage.removeItem('threads_access_token')
        localStorage.removeItem('threads_user_id')
        router.push('/login')
    }

    const handleDeletePost = async (postId: string) => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const response = await fetch(`${API_BASE_URL}/api/posts/${postId}?accessToken=${token}`, {
                method: 'DELETE'
            })

            if (response.ok) {
                // Refresh posts list
                fetchUserData()
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
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    if (isLoading) {
        return (
            <header className="border-b bg-white">
                <div className="container mx-auto px-4 py-3">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                            <h1 className="text-xl font-bold">Threads API</h1>
                        </div>
                        <div className="animate-pulse">
                            <div className="h-8 w-8 bg-gray-200 rounded-full"></div>
                        </div>
                    </div>
                </div>
            </header>
        )
    }

    return (
        <header className="border-b bg-white sticky top-0 z-50">
            <div className="container mx-auto px-4 py-3">
                <div className="flex items-center justify-between">
                    {/* Logo and Navigation */}
                    <div className="flex items-center space-x-6">
                        <h1
                            className="text-xl font-bold cursor-pointer hover:text-blue-600 transition-colors"
                            onClick={() => router.push('/dashboard')}
                        >
                            Threads API
                        </h1>

                        <nav className="hidden md:flex items-center space-x-4">
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => router.push('/dashboard')}
                            >
                                Dashboard
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => router.push('/search')}
                            >
                                <Search className="h-4 w-4 mr-2" />
                                Search
                            </Button>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => router.push('/insights')}
                            >
                                <BarChart3 className="h-4 w-4 mr-2" />
                                Insights
                            </Button>
                        </nav>
                    </div>

                    {/* Right side - Post Management and User Profile */}
                    <div className="flex items-center space-x-4">
                        {/* Post Management Dropdown */}
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="outline" size="sm">
                                    <FileText className="h-4 w-4 mr-2" />
                                    Posts
                                    {postStats && (
                                        <Badge variant="secondary" className="ml-2">
                                            {postStats.totalPosts}
                                        </Badge>
                                    )}
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-80">
                                <DropdownMenuLabel>
                                    <div className="flex items-center justify-between">
                                        <span>Post Management</span>
                                        <Button
                                            size="sm"
                                            onClick={() => router.push('/create-post')}
                                        >
                                            <Plus className="h-4 w-4 mr-1" />
                                            New
                                        </Button>
                                    </div>
                                </DropdownMenuLabel>

                                {postStats && (
                                    <div className="px-2 py-2 border-b">
                                        <div className="grid grid-cols-3 gap-2 text-sm">
                                            <div className="text-center">
                                                <div className="font-semibold text-blue-600">
                                                    {formatNumber(postStats.totalPosts)}
                                                </div>
                                                <div className="text-gray-500">Posts</div>
                                            </div>
                                            <div className="text-center">
                                                <div className="font-semibold text-green-600">
                                                    {formatNumber(postStats.totalViews)}
                                                </div>
                                                <div className="text-gray-500">Views</div>
                                            </div>
                                            <div className="text-center">
                                                <div className="font-semibold text-red-600">
                                                    {formatNumber(postStats.totalLikes)}
                                                </div>
                                                <div className="text-gray-500">Likes</div>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                <DropdownMenuSeparator />

                                <div className="max-h-64 overflow-y-auto">
                                    {recentPosts.length > 0 ? (
                                        recentPosts.map((post) => (
                                            <DropdownMenuItem key={post.id} className="flex-col items-start p-3">
                                                <div className="w-full">
                                                    <div className="flex items-start justify-between">
                                                        <div className="flex-1 min-w-0">
                                                            <p className="text-sm font-medium text-gray-900 truncate">
                                                                {post.text.length > 50 ? `${post.text.substring(0, 50)}...` : post.text}
                                                            </p>
                                                            <p className="text-xs text-gray-500 mt-1">
                                                                {formatDate(post.timestamp)}
                                                            </p>
                                                        </div>
                                                        <div className="flex items-center space-x-1 ml-2">
                                                            <Button
                                                                size="sm"
                                                                variant="ghost"
                                                                onClick={(e) => {
                                                                    e.stopPropagation()
                                                                    router.push(`/post/${post.id}`)
                                                                }}
                                                            >
                                                                <Eye className="h-3 w-3" />
                                                            </Button>
                                                            <Button
                                                                size="sm"
                                                                variant="ghost"
                                                                onClick={(e) => {
                                                                    e.stopPropagation()
                                                                    handleDeletePost(post.id)
                                                                }}
                                                            >
                                                                <Trash2 className="h-3 w-3 text-red-500" />
                                                            </Button>
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center space-x-4 mt-2">
                                                        <div className="flex items-center text-xs text-gray-500">
                                                            <Eye className="h-3 w-3 mr-1" />
                                                            {formatNumber(post.viewsCount)}
                                                        </div>
                                                        <div className="flex items-center text-xs text-gray-500">
                                                            <TrendingUp className="h-3 w-3 mr-1" />
                                                            {formatNumber(post.likesCount)}
                                                        </div>
                                                        <Badge variant="outline" className="text-xs">
                                                            {post.mediaType}
                                                        </Badge>
                                                    </div>
                                                </div>
                                            </DropdownMenuItem>
                                        ))
                                    ) : (
                                        <DropdownMenuItem disabled>
                                            <span className="text-gray-500">No posts yet</span>
                                        </DropdownMenuItem>
                                    )}
                                </div>

                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={() => router.push('/posts')}>
                                    <FileText className="h-4 w-4 mr-2" />
                                    View All Posts
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>

                        {/* User Profile Dropdown */}
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" className="relative h-8 w-8 rounded-full">
                                    <Avatar className="h-8 w-8">
                                        <AvatarImage
                                            src={userProfile?.threads_profile_picture_url}
                                            alt={userProfile?.username || 'User'}
                                        />
                                        <AvatarFallback>
                                            {userProfile?.username?.charAt(0).toUpperCase() || 'U'}
                                        </AvatarFallback>
                                    </Avatar>
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent className="w-56" align="end" forceMount>
                                <DropdownMenuLabel className="font-normal">
                                    <div className="flex flex-col space-y-1">
                                        <p className="text-sm font-medium leading-none">
                                            {userProfile?.name || userProfile?.username}
                                        </p>
                                        <p className="text-xs leading-none text-muted-foreground">
                                            @{userProfile?.username}
                                        </p>
                                        {userProfile?.threads_biography && (
                                            <p className="text-xs text-gray-500 mt-1 max-w-[200px] truncate">
                                                {userProfile.threads_biography}
                                            </p>
                                        )}
                                    </div>
                                </DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={() => router.push('/profile')}>
                                    <User className="mr-2 h-4 w-4" />
                                    <span>Profile</span>
                                </DropdownMenuItem>
                                <DropdownMenuItem onClick={() => router.push('/settings')}>
                                    <Settings className="mr-2 h-4 w-4" />
                                    <span>Settings</span>
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={handleLogout}>
                                    <LogOut className="mr-2 h-4 w-4" />
                                    <span>Log out</span>
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>
        </header>
    )
} 