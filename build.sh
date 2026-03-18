#!/bin/bash

# Build script for Threads API Docker deployment
# Usage: ./build.sh [dev|prod]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install it and try again."
    exit 1
fi

# Determine environment
ENV=${1:-prod}

if [ "$ENV" = "dev" ]; then
    COMPOSE_FILE="docker-compose.yaml"
    print_status "Building for development environment..."
elif [ "$ENV" = "prod" ]; then
    COMPOSE_FILE="docker-compose.prod.yaml"
    print_status "Building for production environment..."
else
    print_error "Invalid environment. Use 'dev' or 'prod'."
    exit 1
fi

# Clean up previous builds
print_status "Cleaning up previous builds..."
docker-compose -f $COMPOSE_FILE down --remove-orphans || true
docker system prune -f || true

# Build images
print_status "Building Docker images..."
docker-compose -f $COMPOSE_FILE build --no-cache

# Start services
print_status "Starting services..."
docker-compose -f $COMPOSE_FILE up -d

# Wait for services to be healthy
print_status "Waiting for services to be healthy..."
sleep 10

# Check service status
print_status "Checking service status..."
docker-compose -f $COMPOSE_FILE ps

# Show logs
print_status "Showing recent logs..."
docker-compose -f $COMPOSE_FILE logs --tail=50

print_status "Build and deployment completed!"
print_status "API available at: http://localhost:10081"
print_status "Frontend available at: http://localhost:3000"
print_status ""
print_status "To view logs: docker-compose -f $COMPOSE_FILE logs -f"
print_status "To stop: docker-compose -f $COMPOSE_FILE down" 