#!/bin/bash

# FunHub Installation Script
# This script sets up the FunHub application for first-time use

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_CMD="python3"
NODE_VERSION_REQUIRED="18.0.0"

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

# Compare semantic versions
version_ge() {
    [ "$(printf '%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

# Check system requirements
check_requirements() {
    print_header "Checking System Requirements"
    echo ""

    local has_errors=0

    # Check Python
    print_info "Checking Python..."
    if command -v $PYTHON_CMD &> /dev/null; then
        PYTHON_VERSION=$($PYTHON_CMD --version 2>&1 | awk '{print $2}')
        print_success "Python $PYTHON_VERSION found"
    else
        print_error "Python 3 is not installed"
        has_errors=1
    fi

    # Check Node.js
    print_info "Checking Node.js..."
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version | sed 's/v//')
        if version_ge "$NODE_VERSION" "$NODE_VERSION_REQUIRED"; then
            print_success "Node.js v$NODE_VERSION found"
        else
            print_error "Node.js v$NODE_VERSION is too old (requires >= $NODE_VERSION_REQUIRED)"
            has_errors=1
        fi
    else
        print_error "Node.js is not installed"
        has_errors=1
    fi

    # Check npm
    print_info "Checking npm..."
    if command -v npm &> /dev/null; then
        NPM_VERSION=$(npm --version)
        print_success "npm v$NPM_VERSION found"
    else
        print_error "npm is not installed"
        has_errors=1
    fi

    # Check ffmpeg (optional but recommended)
    print_info "Checking ffmpeg..."
    if command -v ffmpeg &> /dev/null; then
        FFMPEG_VERSION=$(ffmpeg -version 2>&1 | head -n1 | awk '{print $3}')
        print_success "ffmpeg $FFMPEG_VERSION found"
    else
        print_warning "ffmpeg not found (required for video thumbnails)"
        print_info "Install with: sudo apt-get install ffmpeg (Ubuntu/Debian)"
        print_info "             sudo yum install ffmpeg (CentOS/RHEL)"
        print_info "             brew install ffmpeg (macOS)"
    fi

    if [ $has_errors -eq 1 ]; then
        echo ""
        print_error "Please install the missing requirements and try again"
        exit 1
    fi
}

# Create directory structure
create_directories() {
    print_header "Creating Directory Structure"
    echo ""

    mkdir -p "$PROJECT_DIR/storage/thumbnails"
    mkdir -p "$PROJECT_DIR/storage/database"
    mkdir -p "$PROJECT_DIR/logs"

    print_success "Directories created"
}

# Setup backend
setup_backend() {
    print_header "Setting Up Backend"
    echo ""

    cd "$PROJECT_DIR/backend"

    # Create virtual environment
    if [ ! -d "venv" ]; then
        print_info "Creating Python virtual environment..."
        $PYTHON_CMD -m venv venv
        print_success "Virtual environment created"
    else
        print_info "Virtual environment already exists"
    fi

    # Activate virtual environment
    source venv/bin/activate

    # Upgrade pip
    print_info "Upgrading pip..."
    pip install --upgrade pip

    # Install dependencies
    print_info "Installing Python dependencies..."
    pip install -r requirements.txt
    print_success "Dependencies installed"

    # Initialize database
    print_info "Initializing database..."
    $PYTHON_CMD -c "
from app import create_app, db
from app.models import Source, Video, Image

app = create_app()
with app.app_context():
    db.create_all()
    print('Database initialized')
"
    print_success "Database initialized"

    deactivate
}

# Setup frontend
setup_frontend() {
    print_header "Setting Up Frontend"
    echo ""

    cd "$PROJECT_DIR/frontend"

    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_info "Installing Node.js dependencies (this may take a while)..."
        npm install
        print_success "Dependencies installed"
    else
        print_info "Dependencies already installed"
    fi
}

# Make scripts executable
setup_scripts() {
    print_header "Setting Up Scripts"
    echo ""

    chmod +x "$PROJECT_DIR/start-backend.sh"
    chmod +x "$PROJECT_DIR/start-frontend.sh"
    chmod +x "$PROJECT_DIR/start-all.sh"
    chmod +x "$PROJECT_DIR/stop-all.sh"

    print_success "Scripts are now executable"
}

# Print completion message
print_completion() {
    echo ""
    print_header "========================================"
    print_header "  🎉 FunHub Installation Complete!"
    print_header "========================================"
    echo ""
    print_success "FunHub has been successfully installed!"
    echo ""
    print_info "To start FunHub:"
    echo "  ./start-all.sh     - Start both backend and frontend"
    echo "  ./start-backend.sh - Start backend only"
    echo "  ./start-frontend.sh - Start frontend only"
    echo ""
    print_info "To stop FunHub:"
    echo "  ./stop-all.sh      - Stop all services"
    echo ""
    print_info "Access URLs (when running):"
    echo "  Frontend: http://localhost:5173"
    echo "  Backend:  http://localhost:5000"
    echo ""
}

# Main execution
main() {
    echo "========================================"
    echo "  FunHub Installation Script"
    echo "========================================"
    echo ""

    check_requirements
    create_directories
    setup_backend
    setup_frontend
    setup_scripts
    print_completion
}

# Run main function
main "$@"
