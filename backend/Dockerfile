FROM jrottenberg/ffmpeg:7.1-slim

# Install additional system dependencies
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
    && rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Set working directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create necessary directories
RUN mkdir -p /app/storage/thumbnails /app/storage/database

# Set environment variables
ENV FLASK_APP=run.py
ENV FLASK_ENV=production
ENV PYTHONUNBUFFERED=1
ENV PATH=/usr/local/bin:$PATH

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:5000/api/health || exit 1

# Run the application
CMD ["python3", "run.py"]
