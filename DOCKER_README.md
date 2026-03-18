# Docker Deployment Guide

This guide explains how to deploy the Threads API application using Docker containers with Spring Boot native image and Next.js 15 frontend.

## Architecture

The application consists of two main services:

1. **Spring Boot API Backend** - GraalVM native image for fast startup and low memory usage
2. **Next.js 15 Frontend** - Optimized standalone build for production deployment

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM for building native images
- At least 2GB disk space

## Quick Start

### Production Deployment

```bash
# Build and start production services
./build.sh prod

# Or manually:
docker-compose -f docker-compose.prod.yaml up --build -d
```

### Development Deployment

```bash
# Build and start development services (includes nginx proxy)
./build.sh dev

# Or manually:
docker-compose -f docker-compose.yaml up --build -d
```

## Service Details

### Spring Boot API (Port 10081)

- **Image**: Multi-stage build with GraalVM native compilation
- **Base Image**: `ghcr.io/graalvm/graalvm-ce:ol8-java21-22.3.0` (build stage)
- **Runtime Image**: `gcr.io/distroless/base-debian12:nonroot` (runtime stage)
- **Build Time**: ~5-10 minutes (native compilation)
- **Memory Usage**: ~50-100MB (native image)
- **Startup Time**: ~0.1-0.5 seconds

**Health Check**: `http://localhost:10081/actuator/health`

### Next.js Frontend (Port 3000)

- **Image**: Multi-stage build with standalone output
- **Base Image**: `node:22-alpine`
- **Build Time**: ~2-5 minutes
- **Memory Usage**: ~100-200MB
- **Features**: Optimized for production with output tracing

**Health Check**: `http://localhost:3000/api/health`

## Environment Variables

### Spring Boot API

- `SPRING_PROFILES_ACTIVE=prod` - Activates production profile
- `SERVER_PORT=10081` - Server port

### Next.js Frontend

- `NODE_ENV=production` - Production environment
- `NEXT_PUBLIC_API_BASE_URL=https://threads.tadeasfort.com` - API base URL
- `PORT=3000` - Frontend port

## Build Process

### Spring Boot Native Image Build

1. **Dependencies**: Downloads and caches Gradle dependencies
2. **Compilation**: Compiles Java source code
3. **Native Image**: Creates GraalVM native executable
4. **Runtime**: Copies executable to minimal distroless image

### Next.js Build

1. **Dependencies**: Installs npm packages
2. **Build**: Creates optimized production build with standalone output
3. **Runtime**: Copies built files to minimal Node.js runtime

## Commands

### Build and Deploy

```bash
# Production build
./build.sh prod

# Development build
./build.sh dev

# Manual build
docker-compose -f docker-compose.prod.yaml build --no-cache
```

### Management

```bash
# View logs
docker-compose -f docker-compose.prod.yaml logs -f

# Stop services
docker-compose -f docker-compose.prod.yaml down

# Restart services
docker-compose -f docker-compose.prod.yaml restart

# Check status
docker-compose -f docker-compose.prod.yaml ps
```

### Cleanup

```bash
# Remove containers and images
docker-compose -f docker-compose.prod.yaml down --rmi all

# Clean up Docker system
docker system prune -a
```

## File Structure

```
├── Dockerfile.spring          # Spring Boot native image Dockerfile
├── Dockerfile.nextjs          # Next.js frontend Dockerfile
├── docker-compose.yaml        # Development compose file (with nginx)
├── docker-compose.prod.yaml   # Production compose file
├── .dockerignore              # Spring Boot dockerignore
├── src/main/frontend/
│   └── .dockerignore          # Next.js dockerignore
├── build.sh                   # Build and deployment script
└── DOCKER_README.md           # This file
```

## Performance Optimization

### Spring Boot Native Image

- **Startup Time**: ~100x faster than JVM
- **Memory Usage**: ~50% less than JVM
- **Build Time**: Longer due to native compilation
- **Runtime**: No JIT compilation overhead

### Next.js Standalone Output

- **Bundle Size**: Reduced by ~80% with output tracing
- **Cold Start**: Faster due to pre-compiled assets
- **Memory Usage**: Optimized for production workloads

## Troubleshooting

### Common Issues

1. **Out of Memory during build**
   ```bash
   # Increase Docker memory limit to 4GB+
   # Or build with less parallelism
   docker-compose build --parallel 1
   ```

2. **Native image build fails**
   ```bash
   # Check GraalVM compatibility
   # Ensure all dependencies support native compilation
   ```

3. **Frontend build fails**
   ```bash
   # Clear npm cache
   docker-compose exec frontend npm cache clean --force
   ```

### Debug Commands

```bash
# Check container logs
docker logs threads-api
docker logs threads-frontend

# Execute shell in container
docker exec -it threads-api sh
docker exec -it threads-frontend sh

# Check resource usage
docker stats
```

## Security Considerations

- Both containers run as non-root users
- Minimal base images (distroless for Spring Boot)
- No unnecessary packages in runtime images
- Health checks for service monitoring
- Network isolation with Docker networks

## Deployment to Production

1. **Build images on CI/CD**
2. **Push to container registry**
3. **Deploy using docker-compose or Kubernetes**
4. **Configure reverse proxy (nginx/traefik)**
5. **Set up monitoring and logging**

## Monitoring

- Spring Boot Actuator endpoints available at `/actuator/*`
- Docker health checks configured
- Logs available via `docker-compose logs`

## Support

For issues with the Docker setup, check:
1. Docker daemon is running
2. Sufficient memory and disk space
3. Network connectivity
4. Container logs for errors 