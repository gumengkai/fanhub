#!/bin/bash

# FunHub Backend Startup Script
# This script sets up and starts the Flask backend server

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/backend"
VENV_DIR="$BACKEND_DIR/venv"
PYTHON_CMD="python3"
PORT=5000

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

# Check if Python is installed
check_python() {
    print_info "Checking Python installation..."
    if ! command -v $PYTHON_CMD &> /dev/null; then
        print_error "Python 3 is not installed. Please install Python 3.10 or higher."
        exit 1
    fi

    PYTHON_VERSION=$($PYTHON_CMD --version 2>&1 | awk '{print $2}')
    print_success "Found Python $PYTHON_VERSION"
}

# Check if ffmpeg is installed
check_ffmpeg() {
    print_info "Checking ffmpeg installation..."
    if ! command -v ffmpeg &> /dev/null; then
        print_warning "ffmpeg is not installed. Video thumbnail generation will not work."
        print_info "To install ffmpeg:"
        print_info "  Ubuntu/Debian: sudo apt-get install ffmpeg"
        print_info "  macOS: brew install ffmpeg"
        print_info "  CentOS/RHEL: sudo yum install ffmpeg"
    else
        FFMPEG_VERSION=$(ffmpeg -version 2>&1 | head -n1 | awk '{print $3}')
        print_success "Found ffmpeg $FFMPEG_VERSION"
    fi
}

# Create necessary directories
create_directories() {
    print_info "Creating necessary directories..."
    mkdir -p "$BACKEND_DIR/../storage/thumbnails"
    mkdir -p "$BACKEND_DIR/../storage/database"
    print_success "Directories created"
}

# Setup virtual environment
setup_venv() {
    print_info "Setting up Python virtual environment..."

    if [ ! -d "$VENV_DIR" ]; then
        print_info "Creating virtual environment..."
        $PYTHON_CMD -m venv "$VENV_DIR"
        print_success "Virtual environment created"
    else
        print_info "Virtual environment already exists"
    fi

    # Activate virtual environment
    source "$VENV_DIR/bin/activate"
    print_success "Virtual environment activated"
}

# Install dependencies
install_dependencies() {
    print_info "Installing/updating Python dependencies..."

    pip install --upgrade pip

    if [ -f "$BACKEND_DIR/requirements.txt" ]; then
        pip install -r "$BACKEND_DIR/requirements.txt"
        print_success "Dependencies installed"
    else
        print_error "requirements.txt not found"
        exit 1
    fi
}

# Initialize database
init_database() {
    print_info "Initializing database..."
    cd "$BACKEND_DIR"
    $PYTHON_CMD -c "
from app import create_app, db
from app.models import Source, Video, Image

app = create_app()
with app.app_context():
    db.create_all()
    print('Database tables created successfully')
"
    print_success "Database initialized"
}

# Check if port is already in use
check_port() {
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_error "Port $PORT is already in use. Please stop the existing server or change the port."
        exit 1
    fi
}

# Start the server
start_server() {
    print_info "Starting FunHub backend server..."
    print_info "Server will be available at: http://localhost:$PORT"
    print_info "API documentation: http://localhost:$PORT/api/health"
    print_info "Press Ctrl+C to stop the server"
    echo ""

    cd "$BACKEND_DIR"
    export FLASK_APP=run.py
    export FLASK_ENV=development
    export FLASK_DEBUG=1

    # Run the server
    exec $PYTHON_CMD run.py
}

# Cleanup function
cleanup() {
    print_info "Shutting down backend server..."
    deactivate 2>/dev/null || true
    exit 0
}

# Set trap for cleanup
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Backend Startup Script"
    echo "========================================"
    echo ""

    check_python
    check_ffmpeg
    create_directories
    setup_venv
    install_dependencies
    init_database
    check_port
    start_server
}

# Run main function
main "$@"
