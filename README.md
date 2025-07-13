# Threads API Wrapper

A comprehensive Spring Boot wrapper for Meta's Threads API with a modern React frontend, enabling developers to easily integrate Threads functionality into their applications.

## 🚀 Features

- **OAuth Authentication**: Secure Meta OAuth flow with automatic token exchange and refresh
- **Post Creation**: Create and publish text posts with optional images to Threads
- **Post Management**: Retrieve, analyze, and manage posts with insights and analytics
- **Reply Management**: Read and manage replies and conversations
- **Insights & Analytics**: Access comprehensive performance metrics and insights
- **Search Functionality**: Search posts with caching and analytics capabilities
- **User Management**: Retrieve user profiles and thread history
- **Webhook Support**: Handle user deauthorization and deletion events from Meta
- **REST API**: Full REST API with comprehensive endpoints
- **Modern Frontend**: React-based dashboard with shadcn/ui components
- **Documentation**: OpenAPI/Swagger documentation included
- **Environment Configuration**: Easy setup with environment variables

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.5.x** - Modern Java framework
- **Java 21 LTS** - Latest LTS Java version
- **SpringDoc OpenAPI** - API documentation
- **Spring Web MVC** - REST API framework
- **Environment Variables** - Configuration management

### Frontend
- **Next.js 15** - React framework with App Router
- **React 19** - Latest React version
- **Shadcn/ui** - Modern component library
- **Tailwind CSS** - Utility-first CSS framework
- **TypeScript** - Type-safe JavaScript

## 📋 Prerequisites

- Java 21 or higher
- Node.js 18 or higher
- Meta Developer Account
- Threads App configured in Meta Developer Console

## ⚙️ Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd threadsapi
```

### 2. Environment Configuration

Create a `.env` file in the root directory:

```env
META_APP_ID=your_meta_app_id
META_APP_SECRET=your_meta_app_secret
THREADS_APP_ID=your_threads_app_id
THREADS_APP_SECRET=your_threads_app_secret
APP_URL=https://threads.tadeasfort.com
CLIENT_TOKEN=your_client_token
```

### 3. Backend Setup

```bash
# Build the Spring Boot application
./gradlew build

# Run the application
./gradlew bootRun
```

The backend will start on `http://localhost:10081`

### 4. Frontend Setup

```bash
# Navigate to frontend directory
cd src/main/frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3001`

## 🔗 API Endpoints

### Authentication
- `GET /api/auth/login-url` - Get Meta OAuth login URL
- `POST /api/auth/exchange-token` - Exchange authorization code for access token
- `GET /api/auth/callback` - OAuth callback endpoint
- `POST /api/auth/refresh-token` - Refresh long-lived token

### Webhooks
- `POST /api/auth/uninstall` - Handle user deauthorization webhook
- `POST /api/auth/delete` - Handle user deletion webhook
- `GET /api/auth/uninstall` - Webhook verification endpoint
- `GET /api/auth/delete` - Webhook verification endpoint

### Posts
- `POST /api/posts/create` - Create a new post
- `POST /api/posts/publish` - Publish a created post
- `GET /api/posts/user/{userId}` - Get user's posts
- `GET /api/posts/user/{userId}/insights` - Get user's posts with insights
- `DELETE /api/posts/{postId}` - Delete a post
- `GET /api/posts/top/views` - Get top posts by views
- `GET /api/posts/top/likes` - Get top posts by likes

### Search
- `GET /api/search/posts` - Search posts with caching
- `GET /api/search/fresh` - Search posts without cache
- `GET /api/search/history/{userId}` - Get search history
- `GET /api/search/popular` - Get popular search queries
- `GET /api/search/analytics` - Get search analytics

### Insights
- `GET /api/insights/dashboard/{userId}` - Get insights dashboard
- `GET /api/insights/performance/{userId}` - Get performance metrics
- `GET /api/insights/trends/{userId}` - Get trending insights
- `GET /api/insights/user/{userId}` - Fetch fresh insights from API

### User
- `GET /api/user/profile` - Get user profile information
- `GET /api/user/threads` - Get user's threads

## 📖 API Documentation

Once the backend is running, access the interactive API documentation:

- **Swagger UI**: `http://localhost:10081/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:10081/v3/api-docs`

## 🔐 Authentication Flow

1. **Get Login URL**: Call `/api/auth/login-url` to get the Meta OAuth URL
2. **User Authorization**: Redirect user to Meta for authorization
3. **Handle Callback**: Meta redirects to your callback URL with authorization code
4. **Exchange Token**: Use `/api/auth/exchange-token` to get access token
5. **Use API**: Include access token in subsequent API calls

## 💻 Usage Examples

### Getting Started

1. Visit `http://localhost:3001`
2. Click "Get Started" to begin authentication
3. Log in with your Meta account
4. Create and publish posts from the dashboard

### API Usage

```javascript
// Get login URL
const response = await fetch('http://localhost:10081/api/auth/login-url');
const { authUrl } = await response.json();

// Exchange code for token
const tokenResponse = await fetch('http://localhost:10081/api/auth/exchange-token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    code: 'authorization_code',
    redirect_uri: 'your_redirect_uri'
  })
});

// Create a post
const postResponse = await fetch(`http://localhost:10081/api/posts/create?accessToken=${token}`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    media_type: 'TEXT',
    text: 'Hello from the Threads API!'
  })
});
```

## 🏗️ Project Structure

```
threadsapi/
├── src/main/java/com/tadeasfort/threadsapi/
│   ├── config/           # Configuration classes
│   ├── controller/       # REST controllers
│   ├── dto/             # Data Transfer Objects
│   ├── service/         # Business logic services
│   └── ThreadsapiApplication.java
├── src/main/frontend/   # Next.js frontend
│   ├── src/app/         # App Router pages
│   ├── src/components/  # Reusable components
│   └── src/lib/         # Utility functions
├── src/main/resources/
│   └── application.properties
├── build.gradle         # Gradle configuration
└── .env                # Environment variables
```

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server Configuration
server.port=10081

# Threads API Configuration
threads.api.base-url=https://graph.threads.net/v1.0
threads.api.auth-url=https://threads.net/oauth/authorize
threads.api.token-url=https://graph.threads.net/oauth/access_token

# SpringDoc Configuration
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `META_APP_ID` | Meta App ID from Developer Console |
| `META_APP_SECRET` | Meta App Secret |
| `THREADS_APP_ID` | Threads App ID |
| `THREADS_APP_SECRET` | Threads App Secret |
| `APP_URL` | Your application URL |
| `CLIENT_TOKEN` | Meta Client Token |

## 🚀 Deployment

### Backend Deployment

```bash
# Build JAR file
./gradlew bootJar

# Run in production
java -jar build/libs/threadsapi-0.0.1-SNAPSHOT.jar
```

### Frontend Deployment

```bash
# Build for production
npm run build

# Start production server
npm start
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:

1. Check the [API Documentation](http://localhost:10081/swagger-ui.html)
2. Review the [Meta Threads API Documentation](https://developers.facebook.com/docs/threads)
3. Open an issue on GitHub

## 🔮 Future Enhancements

- [ ] Reply management
- [ ] Post analytics and insights
- [ ] Webhook support
- [ ] Bulk post operations
- [ ] Advanced media support (videos, carousels)
- [ ] Rate limiting and queuing
- [ ] User management dashboard
- [ ] Post scheduling

---

Built with ❤️ using Spring Boot and React 