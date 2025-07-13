'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import {
    Plus,
    Search,
    TrendingUp,
    Eye,
    Heart,
    MessageCircle,
    Repeat,
    Quote,
    Calendar,
    AlertCircle,
    Trash2,
    ChevronDown,
    ChevronUp,
    ExternalLink,
    RefreshCw
} from 'lucide-react';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081";

interface KeywordSubscription {
    id: number;
    keyword: string;
    searchFrequencyHours: number;
    minEngagementThreshold: number;
    searchType: 'TEXT' | 'HASHTAG' | 'MENTION';
    active: boolean;
    createdAt: string;
    lastSearchAt?: string;
}

interface DiscoveredPost {
    id: number;
    threadId: string;
    text: string;
    username: string;
    permalink: string;
    timestamp: string;
    likeCount: number;
    replyCount: number;
    repostCount: number;
    quoteCount: number;
    viewCount?: number;
    engagementScore: number;
    keyword: string;
    discoveredAt: string;
}

interface NewSubscription {
    keyword: string;
    searchFrequencyHours: number;
    minEngagementThreshold: number;
    searchType: 'TEXT' | 'HASHTAG' | 'MENTION';
}

export default function DiscoveryPage() {
    const [subscriptions, setSubscriptions] = useState<KeywordSubscription[]>([]);
    const [discoveredPosts, setDiscoveredPosts] = useState<{ [keyword: string]: DiscoveredPost[] }>({});
    const [expandedKeywords, setExpandedKeywords] = useState<Set<string>>(new Set());
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [showAddDialog, setShowAddDialog] = useState(false);
    const [newSubscription, setNewSubscription] = useState<NewSubscription>({
        keyword: '',
        searchFrequencyHours: 24,
        minEngagementThreshold: 10,
        searchType: 'TEXT'
    });
    const router = useRouter();

    const fetchSubscriptions = useCallback(async () => {
        try {
            const token = localStorage.getItem('threads_access_token');
            const userId = localStorage.getItem('threads_user_id');

            if (!token || !userId) {
                router.push('/login');
                return;
            }

            const response = await fetch(`${API_BASE_URL}/api/automation/subscriptions?userId=${userId}&accessToken=${token}`);
            if (response.ok) {
                const data = await response.json();
                setSubscriptions(data);
                // Fetch discovered posts for each subscription
                data.forEach((sub: KeywordSubscription) => {
                    fetchDiscoveredPosts(sub.keyword);
                });
            }
        } catch (error) {
            console.error('Error fetching subscriptions:', error);
        } finally {
            setIsLoading(false);
        }
    }, [router]);

    useEffect(() => {
        fetchSubscriptions();
    }, [fetchSubscriptions]);

    const fetchDiscoveredPosts = async (keyword: string) => {
        try {
            const token = localStorage.getItem('threads_access_token');
            const userId = localStorage.getItem('threads_user_id');

            if (!token || !userId) return;

            const response = await fetch(`${API_BASE_URL}/api/automation/discovered-posts/${userId}?keyword=${encodeURIComponent(keyword)}&limit=10&sortBy=engagementScore&accessToken=${token}`);
            if (response.ok) {
                const posts = await response.json();
                setDiscoveredPosts(prev => ({
                    ...prev,
                    [keyword]: posts
                }));
            }
        } catch (error) {
            console.error(`Error fetching posts for keyword ${keyword}:`, error);
        }
    };

    const addSubscription = async () => {
        if (!newSubscription.keyword.trim()) return;

        try {
            const token = localStorage.getItem('threads_access_token');
            const userId = localStorage.getItem('threads_user_id');

            if (!token || !userId) {
                router.push('/login');
                return;
            }

            const response = await fetch(`${API_BASE_URL}/api/automation/subscriptions?userId=${userId}&accessToken=${token}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(newSubscription)
            });

            if (response.ok) {
                const subscription = await response.json();
                setSubscriptions(prev => [...prev, subscription]);
                setNewSubscription({
                    keyword: '',
                    searchFrequencyHours: 24,
                    minEngagementThreshold: 10,
                    searchType: 'TEXT'
                });
                setShowAddDialog(false);
                // Fetch posts for the new keyword
                fetchDiscoveredPosts(subscription.keyword);
            }
        } catch (error) {
            console.error('Error adding subscription:', error);
        }
    };

    const deleteSubscription = async (id: number, keyword: string) => {
        try {
            const token = localStorage.getItem('threads_access_token');
            const userId = localStorage.getItem('threads_user_id');

            if (!token || !userId) return;

            const response = await fetch(`${API_BASE_URL}/api/automation/subscriptions/${id}?userId=${userId}&accessToken=${token}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                setSubscriptions(prev => prev.filter(sub => sub.id !== id));
                setDiscoveredPosts(prev => {
                    const updated = { ...prev };
                    delete updated[keyword];
                    return updated;
                });
                setExpandedKeywords(prev => {
                    const updated = new Set(prev);
                    updated.delete(keyword);
                    return updated;
                });
            }
        } catch (error) {
            console.error('Error deleting subscription:', error);
        }
    };

    const toggleKeywordExpansion = (keyword: string) => {
        setExpandedKeywords(prev => {
            const updated = new Set(prev);
            if (updated.has(keyword)) {
                updated.delete(keyword);
            } else {
                updated.add(keyword);
            }
            return updated;
        });
    };

    const triggerManualSearch = async (keyword: string) => {
        try {
            const token = localStorage.getItem('threads_access_token');
            const userId = localStorage.getItem('threads_user_id');

            if (!token || !userId) return;

            setIsRefreshing(true);
            await fetch(`${API_BASE_URL}/api/automation/search?userId=${userId}&accessToken=${token}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ keyword })
            });
            // Refresh discovered posts after manual search
            setTimeout(() => fetchDiscoveredPosts(keyword), 2000);
        } catch (error) {
            console.error('Error triggering manual search:', error);
        } finally {
            setIsRefreshing(false);
        }
    };

    const formatEngagement = (post: DiscoveredPost) => {
        return post.likeCount + post.replyCount + post.repostCount + post.quoteCount;
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const getSearchTypeIcon = (type: string) => {
        switch (type) {
            case 'HASHTAG': return '#';
            case 'MENTION': return '@';
            default: return 'T';
        }
    };

    const refreshAllData = async () => {
        setIsRefreshing(true);
        await fetchSubscriptions();
        setIsRefreshing(false);
    };

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
        );
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="container mx-auto px-4 py-8">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Content Discovery</h1>
                        <p className="text-gray-600 mt-2">
                            Monitor keywords and discover trending content automatically
                        </p>
                    </div>
                    <div className="flex items-center gap-4">
                        <Button onClick={refreshAllData} disabled={isRefreshing} variant="outline">
                            <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
                            {isRefreshing ? 'Refreshing...' : 'Refresh Data'}
                        </Button>
                        <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
                            <DialogTrigger asChild>
                                <Button>
                                    <Plus className="h-4 w-4 mr-2" />
                                    Add Keyword
                                </Button>
                            </DialogTrigger>
                            <DialogContent>
                                <DialogHeader>
                                    <DialogTitle>Add New Keyword Subscription</DialogTitle>
                                    <DialogDescription>
                                        Create a new keyword subscription to monitor trending content
                                    </DialogDescription>
                                </DialogHeader>
                                <div className="grid gap-4 py-4">
                                    <div className="grid grid-cols-4 items-center gap-4">
                                        <Label htmlFor="keyword" className="text-right">
                                            Keyword
                                        </Label>
                                        <Input
                                            id="keyword"
                                            value={newSubscription.keyword}
                                            onChange={(e) => setNewSubscription(prev => ({ ...prev, keyword: e.target.value }))}
                                            placeholder="Enter keyword..."
                                            className="col-span-3"
                                        />
                                    </div>
                                    <div className="grid grid-cols-4 items-center gap-4">
                                        <Label htmlFor="searchType" className="text-right">
                                            Search Type
                                        </Label>
                                        <Select
                                            value={newSubscription.searchType}
                                            onValueChange={(value: 'TEXT' | 'HASHTAG' | 'MENTION') =>
                                                setNewSubscription(prev => ({ ...prev, searchType: value }))
                                            }
                                        >
                                            <SelectTrigger className="col-span-3">
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="TEXT">Text</SelectItem>
                                                <SelectItem value="HASHTAG">Hashtag</SelectItem>
                                                <SelectItem value="MENTION">Mention</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>
                                    <div className="grid grid-cols-4 items-center gap-4">
                                        <Label htmlFor="frequency" className="text-right">
                                            Frequency (hours)
                                        </Label>
                                        <Input
                                            id="frequency"
                                            type="number"
                                            min="1"
                                            max="168"
                                            value={newSubscription.searchFrequencyHours}
                                            onChange={(e) => setNewSubscription(prev => ({ ...prev, searchFrequencyHours: parseInt(e.target.value) }))}
                                            className="col-span-3"
                                        />
                                    </div>
                                    <div className="grid grid-cols-4 items-center gap-4">
                                        <Label htmlFor="engagement" className="text-right">
                                            Min Engagement
                                        </Label>
                                        <Input
                                            id="engagement"
                                            type="number"
                                            min="0"
                                            value={newSubscription.minEngagementThreshold}
                                            onChange={(e) => setNewSubscription(prev => ({ ...prev, minEngagementThreshold: parseInt(e.target.value) }))}
                                            className="col-span-3"
                                        />
                                    </div>
                                </div>
                                <DialogFooter>
                                    <Button onClick={addSubscription} disabled={!newSubscription.keyword.trim()}>
                                        Add Subscription
                                    </Button>
                                </DialogFooter>
                            </DialogContent>
                        </Dialog>
                    </div>
                </div>

                {/* Subscriptions List */}
                <div className="space-y-6">
                    {subscriptions.length === 0 ? (
                        <Card>
                            <CardContent className="text-center py-12">
                                <Search className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                                <CardTitle className="mb-2">No keyword subscriptions yet</CardTitle>
                                <CardDescription className="mb-4">
                                    Add your first keyword to start discovering trending content
                                </CardDescription>
                                <Button onClick={() => setShowAddDialog(true)}>
                                    <Plus className="h-4 w-4 mr-2" />
                                    Add Keyword
                                </Button>
                            </CardContent>
                        </Card>
                    ) : (
                        subscriptions.map((subscription) => {
                            const posts = discoveredPosts[subscription.keyword] || [];
                            const isExpanded = expandedKeywords.has(subscription.keyword);

                            return (
                                <Card key={subscription.id}>
                                    <CardHeader>
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-4">
                                                <div className="flex items-center gap-2">
                                                    <Badge variant="secondary" className="w-8 h-8 flex items-center justify-center p-0">
                                                        {getSearchTypeIcon(subscription.searchType)}
                                                    </Badge>
                                                    <div>
                                                        <CardTitle className="text-lg">
                                                            {subscription.keyword}
                                                        </CardTitle>
                                                        <div className="flex items-center gap-4 text-sm text-gray-600">
                                                            <span className="flex items-center gap-1">
                                                                <Calendar className="w-4 h-4" />
                                                                Every {subscription.searchFrequencyHours}h
                                                            </span>
                                                            <span className="flex items-center gap-1">
                                                                <TrendingUp className="w-4 h-4" />
                                                                Min {subscription.minEngagementThreshold} engagement
                                                            </span>
                                                            {subscription.lastSearchAt && (
                                                                <span>Last search: {formatDate(subscription.lastSearchAt)}</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    <Badge variant={subscription.active ? "default" : "destructive"}>
                                                        {subscription.active ? 'Active' : 'Inactive'}
                                                    </Badge>
                                                    {posts.length > 0 && (
                                                        <Badge variant="outline">
                                                            {posts.length} posts found
                                                        </Badge>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <Button
                                                    onClick={() => triggerManualSearch(subscription.keyword)}
                                                    size="sm"
                                                    variant="outline"
                                                    disabled={isRefreshing}
                                                >
                                                    <Search className="w-4 h-4" />
                                                </Button>
                                                <Button
                                                    onClick={() => toggleKeywordExpansion(subscription.keyword)}
                                                    size="sm"
                                                    variant="outline"
                                                    disabled={posts.length === 0}
                                                >
                                                    {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                                                </Button>
                                                <Button
                                                    onClick={() => deleteSubscription(subscription.id, subscription.keyword)}
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </Button>
                                            </div>
                                        </div>
                                    </CardHeader>

                                    {/* Discovered Posts Accordion */}
                                    {isExpanded && (
                                        <CardContent className="pt-0">
                                            {posts.length > 0 ? (
                                                <div className="space-y-4">
                                                    <div className="flex items-center gap-2 mb-4">
                                                        <TrendingUp className="w-4 h-4" />
                                                        <h4 className="font-semibold">Top Performing Posts</h4>
                                                    </div>
                                                    {posts.map((post) => (
                                                        <Card key={post.id} className="bg-gray-50">
                                                            <CardContent className="p-4">
                                                                <div className="flex justify-between items-start mb-3">
                                                                    <div className="flex items-center gap-2">
                                                                        <span className="font-medium">@{post.username}</span>
                                                                        <span className="text-sm text-gray-500">
                                                                            {formatDate(post.timestamp)}
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex items-center gap-2">
                                                                        <Badge variant="outline">
                                                                            Score: {post.engagementScore.toFixed(1)}
                                                                        </Badge>
                                                                        <Button
                                                                            asChild
                                                                            size="sm"
                                                                            variant="outline"
                                                                        >
                                                                            <a
                                                                                href={post.permalink}
                                                                                target="_blank"
                                                                                rel="noopener noreferrer"
                                                                            >
                                                                                <ExternalLink className="w-4 h-4" />
                                                                            </a>
                                                                        </Button>
                                                                    </div>
                                                                </div>
                                                                <p className="text-gray-900 mb-3 line-clamp-3">
                                                                    {post.text}
                                                                </p>
                                                                <div className="flex items-center gap-6 text-sm text-gray-600">
                                                                    {post.viewCount && (
                                                                        <span className="flex items-center gap-1">
                                                                            <Eye className="w-4 h-4" />
                                                                            {post.viewCount.toLocaleString()}
                                                                        </span>
                                                                    )}
                                                                    <span className="flex items-center gap-1">
                                                                        <Heart className="w-4 h-4" />
                                                                        {post.likeCount.toLocaleString()}
                                                                    </span>
                                                                    <span className="flex items-center gap-1">
                                                                        <MessageCircle className="w-4 h-4" />
                                                                        {post.replyCount.toLocaleString()}
                                                                    </span>
                                                                    <span className="flex items-center gap-1">
                                                                        <Repeat className="w-4 h-4" />
                                                                        {post.repostCount.toLocaleString()}
                                                                    </span>
                                                                    <span className="flex items-center gap-1">
                                                                        <Quote className="w-4 h-4" />
                                                                        {post.quoteCount.toLocaleString()}
                                                                    </span>
                                                                    <span className="text-xs font-medium">
                                                                        Total: {formatEngagement(post).toLocaleString()}
                                                                    </span>
                                                                </div>
                                                            </CardContent>
                                                        </Card>
                                                    ))}
                                                </div>
                                            ) : (
                                                <div className="text-center py-8">
                                                    <AlertCircle className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                                                    <p className="text-gray-600">
                                                        No posts discovered yet for this keyword
                                                    </p>
                                                </div>
                                            )}
                                        </CardContent>
                                    )}
                                </Card>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
} 