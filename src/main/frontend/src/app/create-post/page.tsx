"use client"

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import Header from '@/components/Header'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import {
    Plus,
    Image,
    Video,
    Type,
    Grid3X3,
    ArrowLeft,
    Send,
    Loader2
} from 'lucide-react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:10081"

const postSchema = z.object({
    mediaType: z.enum(['TEXT_POST', 'IMAGE', 'VIDEO', 'CAROUSEL_ALBUM']),
    text: z.string().min(1, "Post text is required").max(500, "Post text must be 500 characters or less"),
    imageUrl: z.string().url("Must be a valid URL").optional().or(z.literal("")),
    videoUrl: z.string().url("Must be a valid URL").optional().or(z.literal("")),
    altText: z.string().max(100, "Alt text must be 100 characters or less").optional(),
    locationName: z.string().max(50, "Location name must be 50 characters or less").optional(),
    replyToId: z.string().optional(),
    quotePostId: z.string().optional(),
    allowCommenting: z.boolean(),
    hideLikeViewCounts: z.boolean(),
})

type PostFormData = z.infer<typeof postSchema>

interface PostTemplate {
    name: string
    description: string
    mediaType: string
    icon: React.ReactNode
    example: Partial<PostFormData>
}

export default function CreatePostPage() {
    const [isLoading, setIsLoading] = useState(false)
    const [isPublishing, setIsPublishing] = useState(false)
    const [creationId, setCreationId] = useState<string | null>(null)
    const [publishedPostId, setPublishedPostId] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [success, setSuccess] = useState<string | null>(null)
    const router = useRouter()

    const form = useForm<PostFormData>({
        resolver: zodResolver(postSchema),
        defaultValues: {
            mediaType: 'TEXT_POST',
            text: '',
            imageUrl: '',
            videoUrl: '',
            altText: '',
            locationName: '',
            replyToId: '',
            quotePostId: '',
            allowCommenting: true,
            hideLikeViewCounts: false,
        },
    })

    const watchedMediaType = form.watch('mediaType')

    useEffect(() => {
        // Check if user is authenticated
        const token = localStorage.getItem('threads_access_token')
        if (!token) {
            router.push('/login')
            return
        }
    }, [router])

    const templates: PostTemplate[] = [
        {
            name: 'Text Post',
            description: 'Simple text post',
            mediaType: 'TEXT_POST',
            icon: <Type className="h-4 w-4" />,
            example: {
                mediaType: 'TEXT_POST',
                text: 'What\'s on your mind?',
                allowCommenting: true,
                hideLikeViewCounts: false,
            }
        },
        {
            name: 'Image Post',
            description: 'Post with a single image',
            mediaType: 'IMAGE',
            // eslint-disable-next-line jsx-a11y/alt-text
            icon: <Image className="h-4 w-4" />,
            example: {
                mediaType: 'IMAGE',
                text: 'Check out this image!',
                imageUrl: 'https://example.com/image.jpg',
                altText: 'Description of the image',
                allowCommenting: true,
                hideLikeViewCounts: false,
            }
        },
        {
            name: 'Video Post',
            description: 'Post with a video',
            mediaType: 'VIDEO',
            icon: <Video className="h-4 w-4" />,
            example: {
                mediaType: 'VIDEO',
                text: 'Watch this video!',
                videoUrl: 'https://example.com/video.mp4',
                allowCommenting: true,
                hideLikeViewCounts: false,
            }
        },
        {
            name: 'Carousel Post',
            description: 'Post with multiple images/videos',
            mediaType: 'CAROUSEL_ALBUM',
            icon: <Grid3X3 className="h-4 w-4" />,
            example: {
                mediaType: 'CAROUSEL_ALBUM',
                text: 'Here\'s a collection of images!',
                allowCommenting: true,
                hideLikeViewCounts: false,
            }
        },
    ]

    const applyTemplate = (template: PostTemplate) => {
        form.reset(template.example as PostFormData)
    }

    const createPost = async (data: PostFormData) => {
        const token = localStorage.getItem('threads_access_token')
        const userId = localStorage.getItem('threads_user_id')

        if (!token || !userId) {
            throw new Error('Authentication required')
        }

        const requestBody = {
            media_type: data.mediaType,
            text: data.text,
            userId,
            accessToken: token,
            ...(data.imageUrl && { image_url: data.imageUrl }),
            ...(data.videoUrl && { video_url: data.videoUrl }),
            ...(data.altText && { alt_text: data.altText }),
            ...(data.locationName && { location_name: data.locationName }),
            ...(data.replyToId && { reply_to_id: data.replyToId }),
            ...(data.quotePostId && { quote_post_id: data.quotePostId }),
            allow_commenting: data.allowCommenting,
            hide_like_view_counts: data.hideLikeViewCounts,
        }

        const response = await fetch(`${API_BASE_URL}/api/automation/posts/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
        })

        if (!response.ok) {
            const errorText = await response.text()
            throw new Error(`Failed to create post: ${errorText}`)
        }

        return response.json()
    }

    const publishPost = async (creationId: string) => {
        const token = localStorage.getItem('threads_access_token')
        const userId = localStorage.getItem('threads_user_id')

        if (!token || !userId) {
            throw new Error('Authentication required')
        }

        const response = await fetch(`${API_BASE_URL}/api/automation/posts/publish`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                creationId,
                userId,
                accessToken: token,
            }),
        })

        if (!response.ok) {
            const errorText = await response.text()
            throw new Error(`Failed to publish post: ${errorText}`)
        }

        return response.json()
    }

    const onSubmit = async (data: PostFormData) => {
        setIsLoading(true)
        setError(null)
        setSuccess(null)

        try {
            // Create the post
            const createResult = await createPost(data)
            setCreationId(createResult.creationId)
            setSuccess(`Post created successfully! Creation ID: ${createResult.creationId}`)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create post')
        } finally {
            setIsLoading(false)
        }
    }

    const handlePublish = async () => {
        if (!creationId) return

        setIsPublishing(true)
        setError(null)

        try {
            const publishResult = await publishPost(creationId)
            setPublishedPostId(publishResult.postId)
            setSuccess(`Post published successfully! Post ID: ${publishResult.postId}`)

            // Reset form after successful publish
            form.reset()
            setCreationId(null)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to publish post')
        } finally {
            setIsPublishing(false)
        }
    }

    const handleCreateAndPublish = async (data: PostFormData) => {
        setIsLoading(true)
        setError(null)
        setSuccess(null)

        try {
            // Create the post
            const createResult = await createPost(data)

            // Immediately publish it
            const publishResult = await publishPost(createResult.creationId)

            setPublishedPostId(publishResult.postId)
            setSuccess(`Post created and published successfully! Post ID: ${publishResult.postId}`)

            // Reset form after successful publish
            form.reset()
            setCreationId(null)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create and publish post')
        } finally {
            setIsLoading(false)
        }
    }

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
                            <h1 className="text-3xl font-bold text-gray-900">Create New Post</h1>
                            <p className="text-gray-600 mt-2">Create and publish content on Threads</p>
                        </div>
                    </div>
                </div>

                {error && (
                    <Alert className="mb-6 border-red-200 bg-red-50">
                        <AlertDescription className="text-red-800">
                            {error}
                        </AlertDescription>
                    </Alert>
                )}

                {success && (
                    <Alert className="mb-6 border-green-200 bg-green-50">
                        <AlertDescription className="text-green-800">
                            {success}
                        </AlertDescription>
                    </Alert>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Post Templates */}
                    <Card className="lg:col-span-1">
                        <CardHeader>
                            <CardTitle className="flex items-center">
                                <Plus className="h-5 w-5 mr-2" />
                                Post Templates
                            </CardTitle>
                            <CardDescription>
                                Choose a template to get started quickly
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-3">
                            {templates.map((template) => (
                                <Button
                                    key={template.name}
                                    variant="outline"
                                    className="w-full justify-start"
                                    onClick={() => applyTemplate(template)}
                                >
                                    {template.icon}
                                    <div className="ml-2 text-left">
                                        <div className="font-medium">{template.name}</div>
                                        <div className="text-xs text-gray-500">{template.description}</div>
                                    </div>
                                </Button>
                            ))}
                        </CardContent>
                    </Card>

                    {/* Main Form */}
                    <Card className="lg:col-span-2">
                        <CardHeader>
                            <CardTitle>Post Details</CardTitle>
                            <CardDescription>
                                Fill in the details for your post
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <Form {...form}>
                                <div className="space-y-6">
                                    {/* Media Type */}
                                    <FormField
                                        control={form.control}
                                        name="mediaType"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>Post Type</FormLabel>
                                                <Select onValueChange={field.onChange} defaultValue={field.value}>
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue placeholder="Select post type" />
                                                        </SelectTrigger>
                                                    </FormControl>
                                                    <SelectContent>
                                                        <SelectItem value="TEXT_POST">Text Post</SelectItem>
                                                        <SelectItem value="IMAGE">Image Post</SelectItem>
                                                        <SelectItem value="VIDEO">Video Post</SelectItem>
                                                        <SelectItem value="CAROUSEL_ALBUM">Carousel Album</SelectItem>
                                                    </SelectContent>
                                                </Select>
                                                <FormDescription>
                                                    Choose the type of content you want to share
                                                </FormDescription>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    {/* Text Content */}
                                    <FormField
                                        control={form.control}
                                        name="text"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>Post Text</FormLabel>
                                                <FormControl>
                                                    <Textarea
                                                        placeholder="What's on your mind?"
                                                        className="min-h-[100px]"
                                                        {...field}
                                                    />
                                                </FormControl>
                                                <FormDescription>
                                                    Share your thoughts (max 500 characters)
                                                </FormDescription>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    {/* Media URL Fields */}
                                    {watchedMediaType === 'IMAGE' && (
                                        <>
                                            <FormField
                                                control={form.control}
                                                name="imageUrl"
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel>Image URL</FormLabel>
                                                        <FormControl>
                                                            <Input
                                                                placeholder="https://example.com/image.jpg"
                                                                {...field}
                                                            />
                                                        </FormControl>
                                                        <FormDescription>
                                                            URL of the image to share
                                                        </FormDescription>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                            <FormField
                                                control={form.control}
                                                name="altText"
                                                render={({ field }) => (
                                                    <FormItem>
                                                        <FormLabel>Alt Text (Optional)</FormLabel>
                                                        <FormControl>
                                                            <Input
                                                                placeholder="Description of the image for accessibility"
                                                                {...field}
                                                            />
                                                        </FormControl>
                                                        <FormDescription>
                                                            Describe the image for accessibility
                                                        </FormDescription>
                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        </>
                                    )}

                                    {watchedMediaType === 'VIDEO' && (
                                        <FormField
                                            control={form.control}
                                            name="videoUrl"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>Video URL</FormLabel>
                                                    <FormControl>
                                                        <Input
                                                            placeholder="https://example.com/video.mp4"
                                                            {...field}
                                                        />
                                                    </FormControl>
                                                    <FormDescription>
                                                        URL of the video to share
                                                    </FormDescription>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    )}

                                    {/* Additional Options */}
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <FormField
                                            control={form.control}
                                            name="locationName"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>Location (Optional)</FormLabel>
                                                    <FormControl>
                                                        <Input
                                                            placeholder="San Francisco, CA"
                                                            {...field}
                                                        />
                                                    </FormControl>
                                                    <FormDescription>
                                                        Tag a location
                                                    </FormDescription>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="replyToId"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>Reply To (Optional)</FormLabel>
                                                    <FormControl>
                                                        <Input
                                                            placeholder="Post ID to reply to"
                                                            {...field}
                                                        />
                                                    </FormControl>
                                                    <FormDescription>
                                                        Reply to another post
                                                    </FormDescription>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />
                                    </div>

                                    <FormField
                                        control={form.control}
                                        name="quotePostId"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>Quote Post (Optional)</FormLabel>
                                                <FormControl>
                                                    <Input
                                                        placeholder="Post ID to quote"
                                                        {...field}
                                                    />
                                                </FormControl>
                                                <FormDescription>
                                                    Quote another post with your commentary
                                                </FormDescription>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    {/* Settings */}
                                    <div className="space-y-4">
                                        <h3 className="text-lg font-medium">Post Settings</h3>

                                        <FormField
                                            control={form.control}
                                            name="allowCommenting"
                                            render={({ field }) => (
                                                <FormItem className="flex flex-row items-center space-x-3 space-y-0">
                                                    <FormControl>
                                                        <Checkbox
                                                            checked={field.value}
                                                            onCheckedChange={field.onChange}
                                                        />
                                                    </FormControl>
                                                    <div className="space-y-1 leading-none">
                                                        <FormLabel>Allow Comments</FormLabel>
                                                        <FormDescription>
                                                            Allow others to comment on this post
                                                        </FormDescription>
                                                    </div>
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="hideLikeViewCounts"
                                            render={({ field }) => (
                                                <FormItem className="flex flex-row items-center space-x-3 space-y-0">
                                                    <FormControl>
                                                        <Checkbox
                                                            checked={field.value}
                                                            onCheckedChange={field.onChange}
                                                        />
                                                    </FormControl>
                                                    <div className="space-y-1 leading-none">
                                                        <FormLabel>Hide Like/View Counts</FormLabel>
                                                        <FormDescription>
                                                            Hide like and view counts from others
                                                        </FormDescription>
                                                    </div>
                                                </FormItem>
                                            )}
                                        />
                                    </div>

                                    {/* Action Buttons */}
                                    <div className="flex flex-col sm:flex-row gap-4 pt-6">
                                        <Button
                                            type="button"
                                            onClick={form.handleSubmit(onSubmit)}
                                            disabled={isLoading}
                                            className="flex-1"
                                            variant="outline"
                                        >
                                            {isLoading ? (
                                                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                            ) : (
                                                <Plus className="h-4 w-4 mr-2" />
                                            )}
                                            {isLoading ? 'Creating...' : 'Create Post'}
                                        </Button>

                                        <Button
                                            type="button"
                                            onClick={form.handleSubmit(handleCreateAndPublish)}
                                            disabled={isLoading}
                                            className="flex-1"
                                        >
                                            {isLoading ? (
                                                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                            ) : (
                                                <Send className="h-4 w-4 mr-2" />
                                            )}
                                            {isLoading ? 'Publishing...' : 'Create & Publish'}
                                        </Button>
                                    </div>

                                    {/* Publish Button (shown after creation) */}
                                    {creationId && !publishedPostId && (
                                        <div className="pt-4 border-t">
                                            <div className="flex items-center justify-between mb-4">
                                                <div>
                                                    <p className="font-medium">Post Created Successfully!</p>
                                                    <p className="text-sm text-gray-600">Creation ID: {creationId}</p>
                                                </div>
                                                <Badge variant="secondary">Ready to Publish</Badge>
                                            </div>
                                            <Button
                                                onClick={handlePublish}
                                                disabled={isPublishing}
                                                className="w-full"
                                            >
                                                {isPublishing ? (
                                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                                ) : (
                                                    <Send className="h-4 w-4 mr-2" />
                                                )}
                                                {isPublishing ? 'Publishing...' : 'Publish Post'}
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            </Form>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    )
} 