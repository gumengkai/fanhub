#!/bin/bash

# FunHub Stop Script
# This script stops all running FunHub services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
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

# Kill process by port
kill_by_port() {
    local port=$1
    local name=$2

    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        local pids=$(lsof -ti:$port)
        print_info "Stopping $name (port $port, PIDs: $pids)..."
        kill $pids 2>/dev/null || kill -9 $pids 2>/dev/null || true
        sleep 1

        # Verify it's stopped
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            print_error "Failed to stop $name"
            return 1
        else
            print_success "$name stopped"
        fi
    else
        print_warning "$name is not running"
    fi
}

# Kill process by PID file
kill_by_pid_file() {
    local pid_file=$1
    local name=$2

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            print_info "Stopping $name (PID: $pid)..."
            kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
            sleep 1
            print_success "$name stopped"
        fi
        rm -f "$pid_file"
    fi
}

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Stop Script"
    echo "========================================"
    echo ""

    PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    # Kill by PID files first
    kill_by_pid_file "$PROJECT_DIR/.backend.pid" "Backend"
    kill_by_pid_file "$PROJECT_DIR/.frontend.pid" "Frontend"

    # Kill by port as fallback
    kill_by_port $BACKEND_PORT "Backend"
    kill_by_port $FRONTEND_PORT "Frontend"

    # Also try to kill any remaining Python/Node processes
    print_info "Cleaning up any remaining processes..."

    # Find and kill Python processes running run.py
    pgrep -f "python.*run.py" | while read pid; do
        kill "$pid" 2>/dev/null || true
    done

    # Find and kill npm/vite processes
    pgrep -f "vite" | while read pid; do
        kill "$pid" 2>/dev/null || true
    done

    echo ""
    print_success "All FunHub services stopped"
}

# Run main function
main "$@"
