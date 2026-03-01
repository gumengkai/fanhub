#!/bin/bash

# FunHub Frontend Startup Script
# This script sets up and starts the React frontend development server

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/frontend"
NODE_VERSION_REQUIRED="18.0.0"
PORT=5173

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

# Compare semantic versions
version_ge() {
    # Returns 0 if $1 >= $2
    [ "$(printf '%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

# Check if Node.js is installed
check_node() {
    print_info "Checking Node.js installation..."

    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js $NODE_VERSION_REQUIRED or higher."
        print_info "Visit: https://nodejs.org/ to download and install Node.js"
        exit 1
    fi

    NODE_VERSION=$(node --version | sed 's/v//')
    print_success "Found Node.js v$NODE_VERSION"

    if ! version_ge "$NODE_VERSION" "$NODE_VERSION_REQUIRED"; then
        print_error "Node.js version $NODE_VERSION is too old. Please upgrade to $NODE_VERSION_REQUIRED or higher."
        exit 1
    fi
}

# Check if npm is installed
check_npm() {
    print_info "Checking npm installation..."

    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install npm."
        exit 1
    fi

    NPM_VERSION=$(npm --version)
    print_success "Found npm v$NPM_VERSION"
}

# Check if backend is running
check_backend() {
    print_info "Checking if backend is running..."

    if curl -s http://localhost:5000/api/health > /dev/null 2>&1; then
        print_success "Backend is running at http://localhost:5000"
    else
        print_warning "Backend is not running at http://localhost:5000"
        print_info "Some features may not work without the backend."
        print_info "Start the backend first with: ./start-backend.sh"
        echo ""
        read -p "Continue without backend? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Install dependencies
install_dependencies() {
    print_info "Installing Node.js dependencies..."

    cd "$FRONTEND_DIR"

    if [ ! -f "package.json" ]; then
        print_error "package.json not found in $FRONTEND_DIR"
        exit 1
    fi

    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_info "Installing dependencies for the first time (this may take a while)..."
        npm install
    else
        print_info "Dependencies already installed, checking for updates..."
        npm install
    fi

    print_success "Dependencies installed"
}

# Check if port is already in use
check_port() {
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_error "Port $PORT is already in use. Please stop the existing server or change the port."
        print_info "You can change the port in vite.config.js"
        exit 1
    fi
}

# Start the development server
start_server() {
    print_info "Starting FunHub frontend development server..."
    print_info "Server will be available at: http://localhost:$PORT"
    print_info "Press Ctrl+C to stop the server"
    echo ""

    cd "$FRONTEND_DIR"
    exec npm run dev
}

# Cleanup function
cleanup() {
    print_info "Shutting down frontend server..."
    exit 0
}

# Set trap for cleanup
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Frontend Startup Script"
    echo "========================================"
    echo ""

    check_node
    check_npm
    check_backend
    install_dependencies
    check_port
    start_server
}

# Run main function
main "$@"
