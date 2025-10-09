-- Add crawler_found column to track whether place was found by crawler
ALTER TABLE places ADD COLUMN IF NOT EXISTS crawler_found BOOLEAN DEFAULT NULL;

-- Add comment to explain the column
COMMENT ON COLUMN places.crawler_found IS 'Indicates whether the place was successfully found by the crawler (true=found, false=not found, null=not yet processed)';
