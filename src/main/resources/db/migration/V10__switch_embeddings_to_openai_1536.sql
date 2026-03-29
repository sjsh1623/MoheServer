-- Switch embedding dimensions from Kanana 1792 to OpenAI text-embedding-3-small 1536

-- Clear existing embeddings (incompatible dimensions)
TRUNCATE TABLE place_keyword_embeddings;
TRUNCATE TABLE place_menu_embeddings;

-- Drop old indexes
DROP INDEX IF EXISTS idx_place_keyword_embeddings_embedding;
DROP INDEX IF EXISTS idx_place_menu_embeddings_embedding;

-- Change vector dimensions
ALTER TABLE place_keyword_embeddings ALTER COLUMN embedding SET DATA TYPE vector(1536);
ALTER TABLE place_menu_embeddings ALTER COLUMN embedding SET DATA TYPE vector(1536);

-- Recreate HNSW indexes for cosine similarity search
CREATE INDEX idx_place_keyword_embeddings_embedding
    ON place_keyword_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_place_menu_embeddings_embedding
    ON place_menu_embeddings USING hnsw (embedding vector_cosine_ops);

-- Reset embed_status so batch job re-processes all places
UPDATE places SET embed_status = 'PENDING' WHERE embed_status = 'COMPLETED';

COMMENT ON COLUMN place_keyword_embeddings.embedding IS 'OpenAI text-embedding-3-small 1536-dimensional vector';
COMMENT ON COLUMN place_menu_embeddings.embedding IS 'OpenAI text-embedding-3-small 1536-dimensional vector';
