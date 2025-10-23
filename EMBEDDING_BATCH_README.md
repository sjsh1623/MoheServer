# Keyword Embedding Batch System

This document describes the keyword embedding batch processing system for generating vector embeddings for place keywords.

## Overview

The embedding batch system processes places where `crawler_found = true` and generates vector embeddings for their keywords using the Kanana embedding service. Each keyword is converted into a 1792-dimensional vector and stored in the `place_keyword_embeddings` table for similarity search.

## Architecture

### Components

1. **PlaceKeywordEmbedding Entity** (`entity/PlaceKeywordEmbedding.java`)
   - Stores keyword embeddings with PGVector support
   - Fields: `id`, `place_id`, `keyword`, `embedding`, `created_at`

2. **PlaceKeywordEmbeddingRepository** (`repository/PlaceKeywordEmbeddingRepository.java`)
   - JPA repository for managing embeddings
   - Supports vector similarity search using pgvector

3. **EmbeddingClient** (`service/EmbeddingClient.java`)
   - HTTP client for Kanana embedding service
   - Handles API communication with timeout and error handling

4. **EmbeddingBatchService** (`service/EmbeddingBatchService.java`)
   - Main batch processing logic
   - Processes places in batches of 9
   - Each place processed in separate transaction

5. **EmbeddingBatchController** (`controller/EmbeddingBatchController.java`)
   - REST API endpoints for batch operations
   - Provides status, stats, and control endpoints

### DTOs

- **EmbeddingRequest**: Request payload for embedding service
- **EmbeddingResponse**: Response from embedding service
- **BatchEmbeddingResult**: Result statistics from batch processing

## Database Setup

### 1. Create the table

Run the SQL script to create the `place_keyword_embeddings` table:

```bash
psql -h localhost -p 16239 -U mohe -d mohe_db -f create_place_keyword_embeddings_table.sql
```

Or execute directly:

```sql
CREATE TABLE IF NOT EXISTS public.place_keyword_embeddings (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    keyword TEXT NOT NULL,
    embedding vector(1792),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_place_keyword_embeddings_place
        FOREIGN KEY (place_id)
        REFERENCES public.places(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_place_keyword_embeddings_place_id ON public.place_keyword_embeddings(place_id);
CREATE INDEX idx_place_keyword_embeddings_keyword ON public.place_keyword_embeddings(keyword);
CREATE INDEX idx_place_keyword_embeddings_embedding ON public.place_keyword_embeddings USING hnsw (embedding vector_cosine_ops);
```

### 2. Verify table creation

```sql
\d place_keyword_embeddings
SELECT COUNT(*) FROM place_keyword_embeddings;
```

## Embedding Service Setup

The batch system requires a running Kanana embedding service.

### Starting the Embedding Service

Refer to the MoheEmbedding repository for setup instructions. The service should be running at:

```
http://localhost:8000
```

### API Endpoints

- **POST /embed**: Generate embeddings for text array
  - Request: `{"texts": ["키워드1", "키워드2", ...]}`
  - Response: `{"embeddings": [[0.032, -0.019, ...], ...]}`

- **GET /health**: Health check endpoint

## Configuration

Add to your `.env` file:

```env
# Embedding Service Configuration
EMBEDDING_SERVICE_URL=http://localhost:8000
```

### Application Properties

The RestTemplate is configured with 150-second timeout in `ApplicationConfig.java`:

```java
factory.setConnectTimeout(150000); // 150 seconds
factory.setReadTimeout(150000);    // 150 seconds
```

## Usage

### 1. Check Embedding Service Health

```bash
curl http://localhost:8000/api/batch/embeddings/health
```

Response:
```json
{
  "data": {
    "available": true,
    "serviceUrl": "http://localhost:8000"
  }
}
```

### 2. Run Batch Embedding Process

```bash
curl -X POST http://localhost:8000/api/batch/embeddings/run
```

Response:
```json
{
  "data": {
    "totalPlaces": 100,
    "successfulPlaces": 95,
    "failedPlaces": 2,
    "skippedPlaces": 3,
    "totalEmbeddings": 855,
    "processingTimeMs": 45230,
    "successRate": 95.0,
    "processingTimeSec": 45.23
  }
}
```

### 3. Get Embedding Statistics

```bash
curl http://localhost:8000/api/batch/embeddings/stats
```

Response:
```json
{
  "data": {
    "statistics": "Total embeddings: 855 | Places with embeddings: 95"
  }
}
```

### 4. Delete Embeddings for a Place

```bash
curl -X DELETE http://localhost:8000/api/batch/embeddings/place/123
```

Response:
```json
{
  "data": {
    "placeId": 123,
    "status": "DELETED"
  }
}
```

## Batch Processing Details

### Eligibility Criteria

Places are eligible for embedding if:
- `crawler_found = true`

### Processing Flow

1. **Fetch eligible places**: Query all places with `crawler_found = true`
2. **Batch processing**: Process 9 places at a time
3. **For each place**:
   - Skip if no keywords
   - Take up to 9 keywords
   - Call embedding service with keyword array
   - Validate response
   - Save embeddings to database
4. **Transaction handling**: Each place in separate transaction (REQUIRES_NEW)
5. **Error handling**: Failures logged but don't stop batch

### Logging

The batch process logs progress at multiple levels:

```
[INFO] Start embedding batch process
[INFO] Fetching places where crawler_found = true
[INFO] Found 100 eligible places for embedding
[INFO] Processing batch 1-9 of 100 places
[INFO] Processing place_id=101 (keywords: 5)
[INFO] Successfully embedded 5 keywords
[INFO] Saved 5 embeddings to DB
[INFO] Progress: 10/100 places (10.0%) - Success: 9, Failed: 0, Skipped: 1
...
✅ Embedding batch process completed successfully
[INFO] Total: 100 | Success: 95 | Failed: 2 | Skipped: 3 | Embeddings: 855 | Time: 45230ms
```

### Performance

- **Batch size**: 9 places per batch
- **Keywords per place**: Maximum 9 keywords
- **Request timeout**: 150 seconds
- **Processing rate**: ~2-3 places per second (depends on embedding service)
- **Transaction isolation**: Each place committed independently

## Error Handling

### Common Errors

1. **Embedding service unavailable**
   - Status: 503 SERVICE_UNAVAILABLE
   - Solution: Start the embedding service

2. **No keywords for place**
   - Behavior: Place is skipped
   - Logged as: `[INFO] Skipped place_id=X (no keywords)`

3. **Embedding service error**
   - Behavior: Place marked as failed
   - Logged as: `[ERROR] Embedding service error for place_id=X`
   - Other places continue processing

4. **Timeout**
   - After 150 seconds, request times out
   - Place marked as failed
   - Other places continue processing

### Retry Strategy

Failed places can be reprocessed by:
1. Running the batch again (will process all eligible places)
2. Or deleting embeddings for failed places and running batch

## API Documentation

Full API documentation available at:

```
http://localhost:8000/swagger-ui.html
```

Navigate to "Embedding Batch" section.

## Database Queries

### Check embeddings for a place

```sql
SELECT id, place_id, keyword, created_at
FROM place_keyword_embeddings
WHERE place_id = 101;
```

### Find similar keywords

```sql
SELECT place_id, keyword,
       embedding <=> (SELECT embedding FROM place_keyword_embeddings WHERE id = 1) AS distance
FROM place_keyword_embeddings
WHERE embedding IS NOT NULL
ORDER BY distance
LIMIT 10;
```

### Count embeddings per place

```sql
SELECT place_id, COUNT(*) as embedding_count
FROM place_keyword_embeddings
GROUP BY place_id
ORDER BY embedding_count DESC;
```

### Find places with no embeddings

```sql
SELECT p.id, p.name
FROM places p
WHERE p.crawler_found = true
  AND NOT EXISTS (
    SELECT 1 FROM place_keyword_embeddings pke
    WHERE pke.place_id = p.id
  );
```

## Monitoring

### Check batch progress

Monitor the application logs for real-time progress:

```bash
tail -f logs/application.log | grep "embedding batch"
```

### Check database growth

```sql
SELECT
  COUNT(*) as total_embeddings,
  COUNT(DISTINCT place_id) as places_with_embeddings,
  AVG(embedding_count) as avg_embeddings_per_place
FROM (
  SELECT place_id, COUNT(*) as embedding_count
  FROM place_keyword_embeddings
  GROUP BY place_id
) subquery;
```

## Troubleshooting

### Issue: Embedding service connection refused

**Solution**: Start the embedding service

```bash
cd /path/to/MoheEmbedding
docker-compose up -d
# or
python main.py
```

### Issue: Slow processing

**Possible causes**:
1. Embedding service overloaded
2. Database connection pool exhausted
3. Network latency

**Solutions**:
1. Reduce batch size (modify `BATCH_SIZE` constant)
2. Increase connection pool size in application.yml
3. Check network connectivity

### Issue: Out of memory

**Solution**: Process in smaller batches by modifying the service:

```java
private static final int BATCH_SIZE = 5; // Reduce from 9
```

### Issue: Embeddings dimension mismatch

The system expects 1792-dimensional vectors. If you get dimension errors:

1. Check embedding service model
2. Verify `vector(1792)` in database schema
3. Update schema if using different model

## Future Enhancements

Potential improvements:

1. **Scheduled batch processing**: Add cron job for automatic processing
2. **Parallel processing**: Process multiple batches concurrently
3. **Resume capability**: Save batch state to resume from failures
4. **Metrics dashboard**: Real-time batch processing metrics
5. **Incremental updates**: Only process new/updated places
6. **Batch priority**: Process high-priority places first

## Support

For issues or questions:
1. Check application logs
2. Verify embedding service is running
3. Check database connectivity
4. Review this documentation

## References

- Kanana Model: https://huggingface.co/kakaocorp/kanana-nano-2.1b-embedding
- pgvector Documentation: https://github.com/pgvector/pgvector
- Spring Boot Documentation: https://spring.io/projects/spring-boot
