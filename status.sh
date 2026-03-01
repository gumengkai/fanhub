#!/bin/bash

# FunHub Status Check Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

echo "========================================"
echo "  FunHub Status Check"
echo "========================================"
echo ""

BACKEND_RUNNING=false
FRONTEND_RUNNING=false

# Check Backend
if lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    BACKEND_RUNNING=true
fi

# Check Frontend
if lsof -Pi :5173 -sTCP:LISTEN -t >/dev/null 2>&1; then
    FRONTEND_RUNNING=true
fi

echo "Service Status:"
echo "---------------"

if $BACKEND_RUNNING; then
    print_success "Backend  (port 5000): Running"
    # Check health
    if curl -s http://localhost:5000/api/health > /dev/null 2>&1; then
        print_success "  └─ Health Check: OK"
    else
        print_warning "  └─ Health Check: Failed"
    fi
else
    print_error "Backend  (port 5000): Not Running"
fi

echo ""

if $FRONTEND_RUNNING; then
    print_success "Frontend (port 5173): Running"
else
    print_error "Frontend (port 5173): Not Running"
fi

echo ""
echo "========================================"

# Provide action hints
if $BACKEND_RUNNING && $FRONTEND_RUNNING; then
    echo ""
    print_success "FunHub is fully operational!"
    echo "  Frontend: http://localhost:5173"
    echo "  Backend:  http://localhost:5000"
elif $BACKEND_RUNNING || $FRONTEND_RUNNING; then
    echo ""
    print_warning "FunHub is partially running"
    print_info "To start all services: ./start-all.sh"
else
    echo ""
    print_warning "FunHub is not running"
    print_info "To start: ./start-all.sh"
    print_info "To install: ./install.sh"
fi
