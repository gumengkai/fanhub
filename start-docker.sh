#!/bin/bash

# FunHub Docker Startup Script
# This script starts FunHub using Docker Compose

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Functions
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${PURPLE}$1${NC}"
}

# Check if Docker is installed
check_docker() {
    print_info "Checking Docker installation..."

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        print_info "Please install Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    DOCKER_VERSION=$(docker --version | awk '{print $3}' | tr -d ',')
    print_success "Docker $DOCKER_VERSION found"
}

# Check if Docker Compose is installed
check_docker_compose() {
    print_info "Checking Docker Compose..."

    if ! command -v docker-compose &> /dev/null; then
        if ! docker compose version &> /dev/null 2>&1; then
            print_error "Docker Compose is not installed"
            print_info "Please install Docker Compose: https://docs.docker.com/compose/install/"
            exit 1
        fi
        COMPOSE_CMD="docker compose"
    else
        COMPOSE_CMD="docker-compose"
    fi

    print_success "Docker Compose found"
}

# Build and start containers
start_containers() {
    print_info "Building and starting Docker containers..."

    export COMPOSE_PROJECT_NAME=funhub

    $COMPOSE_CMD up --build -d

    print_success "Containers started"
}

# Wait for services
wait_for_services() {
    print_info "Waiting for services to be ready..."

    local retries=30
    while ! curl -s http://localhost:5000/api/health > /dev/null 2>&1; do
        ((retries--))
        if [ $retries -eq 0 ]; then
            print_error "Services failed to start within timeout"
            print_info "Check logs with: docker-compose logs"
            exit 1
        fi
        sleep 2
    done

    print_success "Services are ready"
}

# Print access information
print_access_info() {
    echo ""
    print_header "========================================"
    print_header "  🎉 FunHub Docker is now running!"
    print_header "========================================"
    echo ""
    print_success "Frontend UI:  http://localhost"
    print_success "Backend API:  http://localhost:5000"
    echo ""
    print_info "Useful commands:"
    echo "  ./start-docker.sh    - Start services"
    echo "  ./stop-docker.sh     - Stop services"
    echo "  docker-compose logs  - View logs"
    echo "  docker-compose ps    - Check status"
    echo ""
}

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Docker Startup Script"
    echo "========================================"
    echo ""

    check_docker
    check_docker_compose
    start_containers
    wait_for_services
    print_access_info
}

# Run main function
main "$@"
