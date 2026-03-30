-- Place description sentence embeddings for prompt-based search
-- One embedding per place (vs keyword embeddings which are 9 per place)

CREATE TABLE IF NOT EXISTS place_description_embeddings (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL UNIQUE,
    description_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_place_desc_embed_place_id
    ON place_description_embeddings(place_id);

CREATE INDEX IF NOT EXISTS idx_place_desc_embed_vector
    ON place_description_embeddings USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE place_description_embeddings IS 'mohe_description 문장 임베딩. 프롬프트 기반 벡터 검색에 사용.';
