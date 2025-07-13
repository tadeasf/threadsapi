'use client';

import { useState, useEffect } from 'react';
import { Plus, Search, TrendingUp, Eye, Heart, MessageCircle, Repeat, Quote, Calendar, AlertCircle, Trash2, ChevronDown, ChevronUp, ExternalLink } from 'lucide-react';

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
    const [loading, setLoading] = useState(true);
    const [showAddForm, setShowAddForm] = useState(false);
    const [newSubscription, setNewSubscription] = useState<NewSubscription>({
        keyword: '',
        searchFrequencyHours: 24,
        minEngagementThreshold: 10,
        searchType: 'TEXT'
    });

    useEffect(() => {
        fetchSubscriptions();
    }, []);

    const fetchSubscriptions = async () => {
        try {
            const response = await fetch('/api/automation/subscriptions', {
                credentials: 'include'
            });
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
            setLoading(false);
        }
    };

    const fetchDiscoveredPosts = async (keyword: string) => {
        try {
            const response = await fetch(`/api/automation/discovered-posts/me?keyword=${encodeURIComponent(keyword)}&limit=10&sortBy=engagementScore`, {
                credentials: 'include'
            });
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
            const response = await fetch('/api/automation/subscriptions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
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
                setShowAddForm(false);
                // Fetch posts for the new keyword
                fetchDiscoveredPosts(subscription.keyword);
            }
        } catch (error) {
            console.error('Error adding subscription:', error);
        }
    };

    const deleteSubscription = async (id: number, keyword: string) => {
        try {
            const response = await fetch(`/api/automation/subscriptions/${id}`, {
                method: 'DELETE',
                credentials: 'include'
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
            await fetch('/api/automation/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ keyword })
            });
            // Refresh discovered posts after manual search
            setTimeout(() => fetchDiscoveredPosts(keyword), 2000);
        } catch (error) {
            console.error('Error triggering manual search:', error);
        }
    };

    const formatEngagement = (post: DiscoveredPost) => {
        return post.likeCount + post.replyCount + post.repostCount + post.quoteCount;
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString();
    };

    const getSearchTypeIcon = (type: string) => {
        switch (type) {
            case 'HASHTAG': return '#';
            case 'MENTION': return '@';
            default: return 'T';
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-6">
                <div className="max-w-6xl mx-auto">
                    <div className="animate-pulse">
                        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/4 mb-6"></div>
                        <div className="space-y-4">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded"></div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-6">
            <div className="max-w-6xl mx-auto">
                {/* Header */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Content Discovery</h1>
                        <p className="text-gray-600 dark:text-gray-400">
                            Monitor keywords and discover trending content automatically
                        </p>
                    </div>
                    <button
                        onClick={() => setShowAddForm(true)}
                        className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 font-medium transition-colors"
                    >
                        <Plus className="w-4 h-4" />
                        Add Keyword
                    </button>
                </div>

                {/* Add Subscription Form */}
                {showAddForm && (
                    <div className="bg-white dark:bg-gray-800 p-6 mb-6 border border-gray-200 dark:border-gray-700">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Add New Keyword Subscription</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    Keyword
                                </label>
                                <input
                                    type="text"
                                    value={newSubscription.keyword}
                                    onChange={(e) => setNewSubscription(prev => ({ ...prev, keyword: e.target.value }))}
                                    placeholder="Enter keyword..."
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    Search Type
                                </label>
                                <select
                                    value={newSubscription.searchType}
                                    onChange={(e) => setNewSubscription(prev => ({ ...prev, searchType: e.target.value as any }))}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                >
                                    <option value="TEXT">Text</option>
                                    <option value="HASHTAG">Hashtag</option>
                                    <option value="MENTION">Mention</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    Search Frequency (hours)
                                </label>
                                <input
                                    type="number"
                                    min="1"
                                    max="168"
                                    value={newSubscription.searchFrequencyHours}
                                    onChange={(e) => setNewSubscription(prev => ({ ...prev, searchFrequencyHours: parseInt(e.target.value) }))}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                    Min Engagement
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    value={newSubscription.minEngagementThreshold}
                                    onChange={(e) => setNewSubscription(prev => ({ ...prev, minEngagementThreshold: parseInt(e.target.value) }))}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                />
                            </div>
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={addSubscription}
                                disabled={!newSubscription.keyword.trim()}
                                className="bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white px-4 py-2 font-medium transition-colors"
                            >
                                Add Subscription
                            </button>
                            <button
                                onClick={() => setShowAddForm(false)}
                                className="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 font-medium transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                )}

                {/* Subscriptions List */}
                <div className="space-y-4">
                    {subscriptions.length === 0 ? (
                        <div className="text-center py-12">
                            <Search className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">No keyword subscriptions yet</h3>
                            <p className="text-gray-600 dark:text-gray-400 mb-4">
                                Add your first keyword to start discovering trending content
                            </p>
                            <button
                                onClick={() => setShowAddForm(true)}
                                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 font-medium transition-colors"
                            >
                                Add Keyword
                            </button>
                        </div>
                    ) : (
                        subscriptions.map((subscription) => {
                            const posts = discoveredPosts[subscription.keyword] || [];
                            const isExpanded = expandedKeywords.has(subscription.keyword);

                            return (
                                <div key={subscription.id} className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 overflow-hidden">
                                    {/* Subscription Header */}
                                    <div className="p-6">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-4">
                                                <div className="flex items-center gap-2">
                                                    <span className="w-8 h-8 bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-400 flex items-center justify-center text-sm font-bold">
                                                        {getSearchTypeIcon(subscription.searchType)}
                                                    </span>
                                                    <div>
                                                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                                                            {subscription.keyword}
                                                        </h3>
                                                        <div className="flex items-center gap-4 text-sm text-gray-600 dark:text-gray-400">
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
                                                    <span className={`px-2 py-1 text-xs font-medium ${subscription.active
                                                            ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
                                                            : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200'
                                                        }`}>
                                                        {subscription.active ? 'Active' : 'Inactive'}
                                                    </span>
                                                    {posts.length > 0 && (
                                                        <span className="px-2 py-1 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200">
                                                            {posts.length} posts found
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => triggerManualSearch(subscription.keyword)}
                                                    className="p-2 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                                                    title="Trigger manual search"
                                                >
                                                    <Search className="w-4 h-4" />
                                                </button>
                                                <button
                                                    onClick={() => toggleKeywordExpansion(subscription.keyword)}
                                                    className="p-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
                                                    disabled={posts.length === 0}
                                                >
                                                    {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                                                </button>
                                                <button
                                                    onClick={() => deleteSubscription(subscription.id, subscription.keyword)}
                                                    className="p-2 text-gray-600 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                                                    title="Delete subscription"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Discovered Posts Accordion */}
                                    {isExpanded && posts.length > 0 && (
                                        <div className="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
                                            <div className="p-6">
                                                <h4 className="text-md font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                                                    <TrendingUp className="w-4 h-4" />
                                                    Top Performing Posts
                                                </h4>
                                                <div className="space-y-4">
                                                    {posts.map((post) => (
                                                        <div key={post.id} className="bg-white dark:bg-gray-800 p-4 border border-gray-200 dark:border-gray-700">
                                                            <div className="flex justify-between items-start mb-3">
                                                                <div className="flex items-center gap-2">
                                                                    <span className="font-medium text-gray-900 dark:text-white">@{post.username}</span>
                                                                    <span className="text-sm text-gray-500 dark:text-gray-400">
                                                                        {formatDate(post.timestamp)}
                                                                    </span>
                                                                </div>
                                                                <div className="flex items-center gap-2">
                                                                    <span className="text-sm font-medium text-blue-600 dark:text-blue-400">
                                                                        Score: {post.engagementScore.toFixed(1)}
                                                                    </span>
                                                                    <a
                                                                        href={post.permalink}
                                                                        target="_blank"
                                                                        rel="noopener noreferrer"
                                                                        className="p-1 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                                                                        title="View original post"
                                                                    >
                                                                        <ExternalLink className="w-4 h-4" />
                                                                    </a>
                                                                </div>
                                                            </div>
                                                            <p className="text-gray-900 dark:text-white mb-3 line-clamp-3">
                                                                {post.text}
                                                            </p>
                                                            <div className="flex items-center gap-6 text-sm text-gray-600 dark:text-gray-400">
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
                                                                <span className="text-xs">
                                                                    Total: {formatEngagement(post).toLocaleString()}
                                                                </span>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    {/* Empty State for No Posts */}
                                    {isExpanded && posts.length === 0 && (
                                        <div className="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-6">
                                            <div className="text-center">
                                                <AlertCircle className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                                                <p className="text-gray-600 dark:text-gray-400">
                                                    No posts discovered yet for this keyword
                                                </p>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
} 