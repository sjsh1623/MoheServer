-- Global keyword → embedding lookup table
-- Prevents duplicate embedding API calls for the same keyword across places
-- 12,218 unique keywords vs 221,636 total instances = 94.5% deduplication

CREATE TABLE IF NOT EXISTS keyword_embeddings (
    id BIGSERIAL PRIMARY KEY,
    keyword TEXT NOT NULL UNIQUE,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_keyword_embeddings_keyword
    ON keyword_embeddings(keyword);

CREATE INDEX IF NOT EXISTS idx_keyword_embeddings_embedding
    ON keyword_embeddings USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE keyword_embeddings IS 'Global keyword-to-vector cache. Avoids redundant OpenAI API calls for identical keywords across places.';
COMMENT ON COLUMN keyword_embeddings.embedding IS 'OpenAI text-embedding-3-small 1536-dimensional vector';
