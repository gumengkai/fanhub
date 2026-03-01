#!/bin/bash

# FunHub Complete Startup Script
# This script starts both backend and frontend servers

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT=5000
FRONTEND_PORT=5173

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

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check Python
    if ! command -v python3 &> /dev/null; then
        print_error "Python 3 is not installed"
        exit 1
    fi

    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed"
        exit 1
    fi

    # Check npm
    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed"
        exit 1
    fi

    print_success "All prerequisites satisfied"
}

# Check if ports are available
check_ports() {
    print_info "Checking port availability..."

    if lsof -Pi :$BACKEND_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_error "Port $BACKEND_PORT is already in use"
        exit 1
    fi

    if lsof -Pi :$FRONTEND_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_error "Port $FRONTEND_PORT is already in use"
        exit 1
    fi

    print_success "Ports are available"
}

# Start backend in background
start_backend() {
    print_info "Starting backend server..."

    # Make sure the script is executable
    chmod +x "$PROJECT_DIR/start-backend.sh"

    # Start backend in a new terminal or background
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal --title="FunHub Backend" -- bash -c "$PROJECT_DIR/start-backend.sh; read -p 'Press Enter to close...'"
    elif command -v konsole &> /dev/null; then
        konsole --new-tab --title="FunHub Backend" -e bash -c "$PROJECT_DIR/start-backend.sh; read -p 'Press Enter to close...'"
    elif command -v xterm &> /dev/null; then
        xterm -T "FunHub Backend" -e bash -c "$PROJECT_DIR/start-backend.sh; read -p 'Press Enter to close...'" &
    else
        # Fallback: run in background and redirect output
        "$PROJECT_DIR/start-backend.sh" > "$PROJECT_DIR/logs/backend.log" 2>&1 &
        BACKEND_PID=$!
        echo $BACKEND_PID > "$PROJECT_DIR/.backend.pid"
        print_info "Backend started in background (PID: $BACKEND_PID)"
        print_info "Logs: $PROJECT_DIR/logs/backend.log"
    fi
}

# Start frontend in background
start_frontend() {
    print_info "Starting frontend server..."

    # Make sure the script is executable
    chmod +x "$PROJECT_DIR/start-frontend.sh"

    # Wait a moment for backend to start
    sleep 3

    # Start frontend in a new terminal or background
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal --title="FunHub Frontend" -- bash -c "$PROJECT_DIR/start-frontend.sh; read -p 'Press Enter to close...'"
    elif command -v konsole &> /dev/null; then
        konsole --new-tab --title="FunHub Frontend" -e bash -c "$PROJECT_DIR/start-frontend.sh; read -p 'Press Enter to close...'"
    elif command -v xterm &> /dev/null; then
        xterm -T "FunHub Frontend" -e bash -c "$PROJECT_DIR/start-frontend.sh; read -p 'Press Enter to close...'" &
    else
        # Fallback: run in background and redirect output
        "$PROJECT_DIR/start-frontend.sh" > "$PROJECT_DIR/logs/frontend.log" 2>&1 &
        FRONTEND_PID=$!
        echo $FRONTEND_PID > "$PROJECT_DIR/.frontend.pid"
        print_info "Frontend started in background (PID: $FRONTEND_PID)"
        print_info "Logs: $PROJECT_DIR/logs/frontend.log"
    fi
}

# Wait for services to be ready
wait_for_services() {
    print_info "Waiting for services to start..."

    # Wait for backend
    local retries=30
    while ! curl -s http://localhost:$BACKEND_PORT/api/health > /dev/null 2>&1; do
        ((retries--))
        if [ $retries -eq 0 ]; then
            print_error "Backend failed to start within timeout"
            exit 1
        fi
        sleep 1
    done
    print_success "Backend is ready"

    # Wait for frontend
    retries=30
    while ! curl -s http://localhost:$FRONTEND_PORT > /dev/null 2>&1; do
        ((retries--))
        if [ $retries -eq 0 ]; then
            print_warning "Frontend may not have started yet, continuing anyway..."
            break
        fi
        sleep 1
    done
    print_success "Frontend is ready"
}

# Print access information
print_access_info() {
    echo ""
    print_header "========================================"
    print_header "  🎉 FunHub is now running!"
    print_header "========================================"
    echo ""
    print_success "Backend API:  http://localhost:$BACKEND_PORT"
    print_success "Frontend UI:  http://localhost:$FRONTEND_PORT"
    echo ""
    print_info "API Health Check: http://localhost:$BACKEND_PORT/api/health"
    echo ""
    print_warning "Press Ctrl+C to stop all services"
    echo ""
}

# Cleanup function
cleanup() {
    print_info "Shutting down FunHub services..."

    # Kill backend if running
    if [ -f "$PROJECT_DIR/.backend.pid" ]; then
        BACKEND_PID=$(cat "$PROJECT_DIR/.backend.pid")
        if kill -0 "$BACKEND_PID" 2>/dev/null; then
            kill "$BACKEND_PID" 2>/dev/null || true
            print_info "Backend stopped"
        fi
        rm -f "$PROJECT_DIR/.backend.pid"
    fi

    # Kill frontend if running
    if [ -f "$PROJECT_DIR/.frontend.pid" ]; then
        FRONTEND_PID=$(cat "$PROJECT_DIR/.frontend.pid")
        if kill -0 "$FRONTEND_PID" 2>/dev/null; then
            kill "$FRONTEND_PID" 2>/dev/null || true
            print_info "Frontend stopped"
        fi
        rm -f "$PROJECT_DIR/.frontend.pid"
    fi

    # Kill any remaining node processes on frontend port
    lsof -ti:$FRONTEND_PORT | xargs kill -9 2>/dev/null || true

    # Kill any remaining python processes on backend port
    lsof -ti:$BACKEND_PORT | xargs kill -9 2>/dev/null || true

    print_success "All services stopped"
    exit 0
}

# Set trap for cleanup
trap cleanup SIGINT SIGTERM

# Create logs directory
mkdir -p "$PROJECT_DIR/logs"

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Complete Startup Script"
    echo "========================================"
    echo ""

    check_prerequisites
    check_ports
    start_backend
    start_frontend
    wait_for_services
    print_access_info

    # Keep script running
    while true; do
        sleep 1
    done
}

# Run main function
main "$@"
