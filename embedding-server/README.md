# Kanana Embedding Server

FastAPI-based embedding service using **kanana-nano-2.1b-embedding** model from OpenLLM-Korea.

## Overview

This service provides OpenAI-compatible embedding API endpoints for generating Korean text embeddings using the kanana-nano-2.1b-embedding model.

## Features

- OpenAI-compatible API endpoints (`/v1/embeddings`)
- Korean language optimized embeddings (1792 dimensions)
- Based on kanana-nano-2.1b-embedding model
- Automatic model download during Docker build
- Health check endpoints

## API Endpoints

### POST /v1/embeddings

OpenAI-compatible embedding generation endpoint.

**Request:**
```json
{
  "input": "텍스트 또는 텍스트 배열",
  "model": "kanana-nano-2.1b-embedding",
  "instruction": "" // Optional instruction prefix
}
```

**Response:**
```json
{
  "object": "list",
  "data": [
    {
      "object": "embedding",
      "embedding": [0.1, 0.2, ...], // 1792-dimensional vector
      "index": 0
    }
  ],
  "model": "kanana-nano-2.1b-embedding",
  "usage": {
    "prompt_tokens": 10,
    "total_tokens": 10
  }
}
```

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy"
}
```

## Usage

### Docker Compose (Recommended)

The embedding service is automatically started with the main application:

```bash
docker compose up --build
```

The service will be available at:
- Docker internal: `http://embedding:8000`
- Host machine: `http://localhost:8001`

### Standalone Docker

```bash
cd embedding-server
docker build -t kanana-embedding .
docker run -p 8001:8000 kanana-embedding
```

### Local Development

```bash
cd embedding-server
pip install -r requirements.txt
python main.py
```

## Environment Variables

- `PORT`: Server port (default: 8000)
- Device selection is automatic:
  - CUDA if available
  - MPS (Apple Silicon) if available
  - CPU otherwise

## Model Information

- **Model**: kakaocorp/kanana-nano-2.1b-embedding
- **Source**: https://huggingface.co/kakaocorp/kanana-nano-2.1b-embedding
- **Architecture**: Custom Kanana2Vec (based on Llama with bidirectional attention)
- **Embedding Dimension**: 1792
- **Language**: Korean-optimized
- **Use Case**: Text embeddings for semantic search and recommendations

## Integration with Spring Boot

The Spring Boot application automatically uses this service when configured:

```yaml
# docker-compose.yml environment variables
EMBEDDING_SERVICE_URL: http://embedding:8000
```

The `OllamaService.vectorizeKeywords()` method has been updated to use this service.

## Performance Notes

- **First startup**: Model is downloaded on first run (~4GB), which may take 3-5 minutes depending on network speed
- Model is cached in Docker volume (`embedding_cache`) for subsequent startups
- First request after startup may be slower due to model loading into memory
- Subsequent requests benefit from cached model in memory
- Supports batch embedding requests

## Important Notes

- The model is downloaded at **runtime** (not during Docker build) to avoid memory issues during build
- The `start_period` in health check is set to 180 seconds to allow time for initial model download
- Model files are persisted in a Docker volume, so they won't be re-downloaded on container restart

## Troubleshooting

Check service health:
```bash
curl http://localhost:8001/health
```

Test embedding generation:
```bash
curl -X POST http://localhost:8001/v1/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "input": "안녕하세요",
    "model": "kanana-nano-2.1b-embedding"
  }'
```

View logs:
```bash
docker compose logs embedding
```
