-- V8: Add status columns for clearer place processing state management
-- crawl_status: PENDING, COMPLETED, FAILED, NOT_FOUND
-- embed_status: PENDING, COMPLETED, FAILED

-- Add new status columns
ALTER TABLE places ADD COLUMN IF NOT EXISTS crawl_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE places ADD COLUMN IF NOT EXISTS embed_status VARCHAR(20) DEFAULT 'PENDING';

-- Migrate existing data from boolean columns to new status columns
-- crawler_found = true -> crawl_status = 'COMPLETED'
-- crawler_found = false -> crawl_status = 'PENDING'
-- crawler_found IS NULL -> crawl_status = 'PENDING'
UPDATE places SET crawl_status = 'COMPLETED' WHERE crawler_found = true;
UPDATE places SET crawl_status = 'PENDING' WHERE crawler_found = false OR crawler_found IS NULL;

-- ready = true -> embed_status = 'COMPLETED'
-- ready = false -> embed_status = 'PENDING'
-- ready IS NULL -> embed_status = 'PENDING'
UPDATE places SET embed_status = 'COMPLETED' WHERE ready = true;
UPDATE places SET embed_status = 'PENDING' WHERE ready = false OR ready IS NULL;

-- Create indexes for efficient querying by status
CREATE INDEX IF NOT EXISTS idx_places_crawl_status ON places(crawl_status);
CREATE INDEX IF NOT EXISTS idx_places_embed_status ON places(embed_status);

-- Drop old boolean columns (no longer needed)
ALTER TABLE places DROP COLUMN IF EXISTS crawler_found;
ALTER TABLE places DROP COLUMN IF EXISTS ready;
