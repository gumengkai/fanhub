#!/bin/bash

# FunHub Quick Start Script
# One-command setup and start for new users

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

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

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Quick Start"
    echo "========================================"
    echo ""

    # Check if already installed
    if [ -d "$PROJECT_DIR/backend/venv" ] && [ -d "$PROJECT_DIR/frontend/node_modules" ]; then
        print_info "FunHub appears to be already installed"
        print_info "Starting services..."
        exec "$PROJECT_DIR/start-all.sh"
    fi

    # Run installation
    print_info "Running first-time installation..."
    "$PROJECT_DIR/install.sh"

    # Start services
    echo ""
    print_info "Installation complete! Starting FunHub..."
    exec "$PROJECT_DIR/start-all.sh"
}

# Run main function
main "$@"
