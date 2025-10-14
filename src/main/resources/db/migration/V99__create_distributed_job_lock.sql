-- ===========================================
-- Distributed Job Lock Table
-- ===========================================
-- This table manages locks for distributed batch processing
-- to prevent multiple workers from processing the same data

CREATE TABLE IF NOT EXISTS distributed_job_lock (
    id BIGSERIAL PRIMARY KEY,

    -- Job identification
    job_name VARCHAR(100) NOT NULL,
    chunk_id VARCHAR(255) NOT NULL,

    -- Worker identification
    worker_id VARCHAR(100) NOT NULL,
    worker_hostname VARCHAR(255),

    -- Lock status
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    -- Possible values: LOCKED, PROCESSING, COMPLETED, FAILED

    -- Timing
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,

    -- Retry tracking
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,

    -- Error tracking
    last_error TEXT,

    -- Metadata
    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint to prevent duplicate locks
    CONSTRAINT uq_job_chunk UNIQUE (job_name, chunk_id)
);

-- Index for querying available chunks
CREATE INDEX idx_job_lock_status ON distributed_job_lock(job_name, status, expires_at);

-- Index for worker queries
CREATE INDEX idx_job_lock_worker ON distributed_job_lock(worker_id, status);

-- Index for cleanup queries
CREATE INDEX idx_job_lock_expires ON distributed_job_lock(expires_at);

-- Comments
COMMENT ON TABLE distributed_job_lock IS 'Manages distributed batch job locks to prevent duplicate processing';
COMMENT ON COLUMN distributed_job_lock.job_name IS 'Name of the batch job (e.g., updateCrawledDataJob)';
COMMENT ON COLUMN distributed_job_lock.chunk_id IS 'Unique identifier for the chunk (e.g., place_1-10)';
COMMENT ON COLUMN distributed_job_lock.worker_id IS 'Unique identifier for the worker machine';
COMMENT ON COLUMN distributed_job_lock.status IS 'Current status: LOCKED, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN distributed_job_lock.expires_at IS 'Lock expiration time (for dead worker cleanup)';
