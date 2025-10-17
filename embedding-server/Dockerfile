FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY main.py .

# Create cache directory for HuggingFace models
RUN mkdir -p /root/.cache/huggingface

# Expose port
EXPOSE 8000

# Health check - longer start period to allow model download on first run
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
# Model will be downloaded on first startup
CMD ["python3", "main.py"]
