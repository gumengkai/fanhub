# FunHub Single-Container Dockerfile
# Combines frontend (React + Nginx) and backend (Flask) in one container

# ============ STAGE 1: BUILD FRONTEND ============
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy package files
COPY frontend/package*.json ./

# Install ALL dependencies (including devDependencies for build)
RUN npm ci && npm cache clean --force

# Copy source code
COPY frontend/ ./

# Build the application
RUN npm run build

# ============ STAGE 2: FINAL IMAGE ============
FROM jrottenberg/ffmpeg:latest

# Override the ffmpeg ENTRYPOINT
ENTRYPOINT []

# Install system dependencies
RUN apt-get update --fix-missing || true && \
    apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    libsm6 \
    libxext6 \
    libglib2.0-0 \
    libgomp1 \
    curl \
    supervisor \
    nginx \
    && rm -rf /var/lib/apt/lists/* && \
    apt-get clean

WORKDIR /app

# Copy built frontend from builder stage
COPY --from=frontend-builder /app/frontend/dist /app/frontend/dist

# ============ SETUP BACKEND ============
WORKDIR /app/backend
COPY backend/requirements.txt /app/backend/

# Create virtual environment and install dependencies
RUN python3 -m venv /app/venv && \
    /app/venv/bin/pip install --no-cache-dir -r requirements.txt

COPY backend/ /app/backend/

# ============ SETUP NGINX ============
# Remove default nginx config
RUN rm -f /etc/nginx/sites-enabled/default

# Copy nginx config (updated for single-container)
COPY nginx-single.conf /etc/nginx/conf.d/default.conf

# ============ SETUP SUPERVISORD ============
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# ============ CREATE DIRECTORIES ============
RUN mkdir -p /app/storage/thumbnails /app/storage/database && \
    mkdir -p /var/log/supervisor

# ============ ENVIRONMENT VARIABLES ============
ENV FLASK_APP=run.py
ENV FLASK_ENV=production
ENV PYTHONUNBUFFERED=1
ENV PATH=/app/venv/bin:/usr/local/bin:$PATH
ENV DATABASE_PATH=/app/storage/database/funhub.db
ENV THUMBNAIL_FOLDER=/app/storage/thumbnails
ENV CORS_ORIGINS=http://localhost,http://127.0.0.1,http://your-domain.com

# ============ EXPOSE PORTS ============
# 8080 - Nginx (Frontend + API Proxy)
# 5000 - Flask Backend (Direct API access for mobile clients)
EXPOSE 8080 5000

# ============ HEALTH CHECK ============
HEALTHCHECK --interval=30s --timeout=30s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# ============ START SUPERVISORD ============
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
