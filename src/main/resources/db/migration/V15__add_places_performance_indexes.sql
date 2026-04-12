-- Performance indexes for places table
-- Speeds up Haversine distance queries by enabling bounding-box pre-filtering

-- Composite index on lat/lon for spatial queries (bounding box filter)
CREATE INDEX IF NOT EXISTS idx_places_lat_lon
    ON places (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Index on embed_status for filtering COMPLETED places
CREATE INDEX IF NOT EXISTS idx_places_embed_status
    ON places (embed_status);

-- Index for popularity sort (review_count DESC, rating DESC)
CREATE INDEX IF NOT EXISTS idx_places_popularity
    ON places (review_count DESC NULLS LAST, rating DESC NULLS LAST)
    WHERE embed_status = 'COMPLETED';

-- Index for rating sort
CREATE INDEX IF NOT EXISTS idx_places_rating
    ON places (rating DESC NULLS LAST)
    WHERE embed_status = 'COMPLETED';
