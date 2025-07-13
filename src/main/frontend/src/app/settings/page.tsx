"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Alert, AlertDescription } from "@/components/ui/alert"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import {
    Settings,
    Bell,
    Shield,
    Database,
    Trash2,
    Download,
    RefreshCw,
    AlertTriangle,
    Key,
    Eye,
    Clock,
    LogOut,
    ExternalLink
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

interface UserProfile {
    id: string
    username: string
    name: string
}

interface AppSettings {
    cacheEnabled: boolean
    cacheDuration: number
    autoRefreshInterval: number
    showNotifications: boolean
    theme: 'light' | 'dark' | 'system'
}

export default function SettingsPage() {
    const [userProfile, setUserProfile] = useState<UserProfile | null>(null)
    const [settings, setSettings] = useState<AppSettings>({
        cacheEnabled: true,
        cacheDuration: 1,
        autoRefreshInterval: 5,
        showNotifications: true,
        theme: 'system'
    })
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [showDeleteDialog, setShowDeleteDialog] = useState(false)
    const router = useRouter()

    useEffect(() => {
        fetchUserData()
        loadSettings()
    }, [])

    const fetchUserData = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')

            if (!token) {
                router.push('/login')
                return
            }

            const profileResponse = await fetch(`${API_BASE_URL}/api/user/profile?accessToken=${token}`)
            if (profileResponse.ok) {
                const profile = await profileResponse.json()
                setUserProfile(profile)
            }

        } catch (error) {
            console.error('Error fetching user data:', error)
        } finally {
            setIsLoading(false)
        }
    }

    const loadSettings = () => {
        const savedSettings = localStorage.getItem('app_settings')
        if (savedSettings) {
            try {
                const parsed = JSON.parse(savedSettings)
                setSettings({ ...settings, ...parsed })
            } catch (error) {
                console.error('Error parsing saved settings:', error)
            }
        }
    }

    const saveSettings = async () => {
        setIsSaving(true)
        try {
            localStorage.setItem('app_settings', JSON.stringify(settings))
            // You could also send settings to backend here if needed
            await new Promise(resolve => setTimeout(resolve, 500)) // Simulate API call
        } catch (error) {
            console.error('Error saving settings:', error)
        } finally {
            setIsSaving(false)
        }
    }

    const handleSettingChange = (key: keyof AppSettings, value: any) => {
        setSettings(prev => ({ ...prev, [key]: value }))
    }

    const handleLogout = () => {
        localStorage.removeItem('threads_access_token')
        localStorage.removeItem('threads_user_id')
        localStorage.removeItem('app_settings')
        router.push('/login')
    }

    const handleRevokeAccess = () => {
        // Open Meta's app permissions page
        window.open('https://www.facebook.com/settings?tab=applications', '_blank')
    }

    const handleClearCache = () => {
        // Clear any cached data
        const keysToKeep = ['threads_access_token', 'threads_user_id', 'app_settings']
        Object.keys(localStorage).forEach(key => {
            if (!keysToKeep.includes(key)) {
                localStorage.removeItem(key)
            }
        })
        alert('Cache cleared successfully!')
    }

    const handleExportData = async () => {
        try {
            const token = localStorage.getItem('threads_access_token')
            const userId = localStorage.getItem('threads_user_id')

            if (!token || !userId) return

            // Fetch user data for export
            const [postsResponse, insightsResponse] = await Promise.all([
                fetch(`${API_BASE_URL}/api/posts/user/${userId}/insights?accessToken=${token}`),
                fetch(`${API_BASE_URL}/api/insights/user/${userId}/dashboard?accessToken=${token}`)
            ])

            const exportData = {
                profile: userProfile,
                posts: postsResponse.ok ? await postsResponse.json() : [],
                insights: insightsResponse.ok ? await insightsResponse.json() : {},
                exportDate: new Date().toISOString()
            }

            const blob = new Blob([JSON.stringify(exportData, null, 2)], {
                type: 'application/json'
            })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = `threads-data-${userProfile?.username}-${new Date().toISOString().split('T')[0]}.json`
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
            URL.revokeObjectURL(url)

        } catch (error) {
            console.error('Error exporting data:', error)
            alert('Failed to export data. Please try again.')
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
                        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
                        <p className="text-gray-600 mt-2">Manage your account preferences and application settings</p>
                    </div>
                    <Button onClick={saveSettings} disabled={isSaving}>
                        {isSaving ? (
                            <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                        ) : (
                            <Settings className="h-4 w-4 mr-2" />
                        )}
                        Save Settings
                    </Button>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Main Settings */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Account Information */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <Shield className="h-5 w-5 mr-2" />
                                    Account Information
                                </CardTitle>
                                <CardDescription>
                                    Your connected Threads account details
                                </CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                        <Label className="text-sm font-medium text-gray-500">Username</Label>
                                        <p className="font-mono text-sm bg-gray-100 p-2 rounded mt-1">
                                            @{userProfile?.username}
                                        </p>
                                    </div>
                                    <div>
                                        <Label className="text-sm font-medium text-gray-500">Display Name</Label>
                                        <p className="font-mono text-sm bg-gray-100 p-2 rounded mt-1">
                                            {userProfile?.name}
                                        </p>
                                    </div>
                                </div>
                                <div className="flex items-center space-x-4 pt-2">
                                    <Button onClick={handleRevokeAccess} variant="outline" size="sm">
                                        <ExternalLink className="h-4 w-4 mr-2" />
                                        Manage App Permissions
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Application Preferences */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <Settings className="h-5 w-5 mr-2" />
                                    Application Preferences
                                </CardTitle>
                                <CardDescription>
                                    Customize how the application behaves
                                </CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-6">
                                {/* Cache Settings */}
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <div className="space-y-0.5">
                                            <Label htmlFor="cache-enabled">Enable Caching</Label>
                                            <p className="text-sm text-gray-500">
                                                Cache search results and API responses for faster loading
                                            </p>
                                        </div>
                                        <Switch
                                            id="cache-enabled"
                                            checked={settings.cacheEnabled}
                                            onCheckedChange={(checked) => handleSettingChange('cacheEnabled', checked)}
                                        />
                                    </div>

                                    {settings.cacheEnabled && (
                                        <div className="space-y-2">
                                            <Label htmlFor="cache-duration">Cache Duration (hours)</Label>
                                            <Select
                                                value={settings.cacheDuration.toString()}
                                                onValueChange={(value) => handleSettingChange('cacheDuration', parseInt(value))}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="1">1 hour</SelectItem>
                                                    <SelectItem value="6">6 hours</SelectItem>
                                                    <SelectItem value="12">12 hours</SelectItem>
                                                    <SelectItem value="24">24 hours</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    )}
                                </div>

                                <Separator />

                                {/* Auto Refresh */}
                                <div className="space-y-2">
                                    <Label htmlFor="auto-refresh">Auto Refresh Interval (minutes)</Label>
                                    <p className="text-sm text-gray-500">
                                        How often to automatically refresh data in the dashboard
                                    </p>
                                    <Select
                                        value={settings.autoRefreshInterval.toString()}
                                        onValueChange={(value) => handleSettingChange('autoRefreshInterval', parseInt(value))}
                                    >
                                        <SelectTrigger className="w-full">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="0">Disabled</SelectItem>
                                            <SelectItem value="1">1 minute</SelectItem>
                                            <SelectItem value="5">5 minutes</SelectItem>
                                            <SelectItem value="15">15 minutes</SelectItem>
                                            <SelectItem value="30">30 minutes</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <Separator />

                                {/* Notifications */}
                                <div className="flex items-center justify-between">
                                    <div className="space-y-0.5">
                                        <Label htmlFor="notifications">Browser Notifications</Label>
                                        <p className="text-sm text-gray-500">
                                            Show browser notifications for important updates
                                        </p>
                                    </div>
                                    <Switch
                                        id="notifications"
                                        checked={settings.showNotifications}
                                        onCheckedChange={(checked) => handleSettingChange('showNotifications', checked)}
                                    />
                                </div>
                            </CardContent>
                        </Card>

                        {/* Data Management */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <Database className="h-5 w-5 mr-2" />
                                    Data Management
                                </CardTitle>
                                <CardDescription>
                                    Manage your stored data and cache
                                </CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <Button onClick={handleClearCache} variant="outline" className="w-full">
                                        <Trash2 className="h-4 w-4 mr-2" />
                                        Clear Cache
                                    </Button>
                                    <Button onClick={handleExportData} variant="outline" className="w-full">
                                        <Download className="h-4 w-4 mr-2" />
                                        Export Data
                                    </Button>
                                </div>
                                <Alert>
                                    <AlertTriangle className="h-4 w-4" />
                                    <AlertDescription>
                                        Clearing cache will remove all stored search results and cached API responses.
                                        This action cannot be undone.
                                    </AlertDescription>
                                </Alert>
                            </CardContent>
                        </Card>
                    </div>

                    {/* Sidebar */}
                    <div className="space-y-6">
                        {/* Quick Stats */}
                        <Card>
                            <CardHeader>
                                <CardTitle className="flex items-center">
                                    <Eye className="h-5 w-5 mr-2" />
                                    Session Info
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-3">
                                <div className="flex items-center justify-between">
                                    <span className="text-sm text-gray-600">Logged in as</span>
                                    <Badge variant="secondary">@{userProfile?.username}</Badge>
                                </div>
                                <div className="flex items-center justify-between">
                                    <span className="text-sm text-gray-600">Session started</span>
                                    <Badge variant="outline">
                                        <Clock className="h-3 w-3 mr-1" />
                                        Today
                                    </Badge>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Quick Actions */}
                        <Card>
                            <CardHeader>
                                <CardTitle>Quick Actions</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-3">
                                <Button
                                    onClick={() => router.push('/profile')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <Shield className="h-4 w-4 mr-2" />
                                    View Profile
                                </Button>
                                <Button
                                    onClick={() => router.push('/dashboard')}
                                    variant="outline"
                                    className="w-full justify-start"
                                >
                                    <Eye className="h-4 w-4 mr-2" />
                                    Dashboard
                                </Button>
                                <Button
                                    onClick={handleLogout}
                                    variant="outline"
                                    className="w-full justify-start text-red-600 hover:text-red-700"
                                >
                                    <LogOut className="h-4 w-4 mr-2" />
                                    Sign Out
                                </Button>
                            </CardContent>
                        </Card>

                        {/* Danger Zone */}
                        <Card className="border-red-200">
                            <CardHeader>
                                <CardTitle className="text-red-600 flex items-center">
                                    <AlertTriangle className="h-5 w-5 mr-2" />
                                    Danger Zone
                                </CardTitle>
                            </CardHeader>
                            <CardContent>
                                <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                                    <DialogTrigger asChild>
                                        <Button variant="destructive" className="w-full">
                                            <Trash2 className="h-4 w-4 mr-2" />
                                            Delete All Data
                                        </Button>
                                    </DialogTrigger>
                                    <DialogContent>
                                        <DialogHeader>
                                            <DialogTitle>Are you absolutely sure?</DialogTitle>
                                            <DialogDescription>
                                                This action will permanently delete all your cached data, settings, and
                                                sign you out of the application. This action cannot be undone.
                                            </DialogDescription>
                                        </DialogHeader>
                                        <DialogFooter>
                                            <Button
                                                variant="outline"
                                                onClick={() => setShowDeleteDialog(false)}
                                            >
                                                Cancel
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                onClick={() => {
                                                    localStorage.clear()
                                                    router.push('/login')
                                                }}
                                            >
                                                Delete Everything
                                            </Button>
                                        </DialogFooter>
                                    </DialogContent>
                                </Dialog>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        </div>
    )
} 