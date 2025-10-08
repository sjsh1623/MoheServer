-- Migration: Remove unused columns from places table
-- Version: V5
-- Description: Remove unnecessary fields that are not being used in the application
-- Author: Claude Code
-- Date: 2025-10-07

-- Remove unused columns from places table
ALTER TABLE places
    DROP COLUMN IF EXISTS first_seen_at,
    DROP COLUMN IF EXISTS last_rating_check,
    DROP COLUMN IF EXISTS is_new_place,
    DROP COLUMN IF EXISTS should_recheck_rating,
    DROP COLUMN IF EXISTS source_flags,
    DROP COLUMN IF EXISTS price_level,
    DROP COLUMN IF EXISTS user_ratings_total,
    DROP COLUMN IF EXISTS telephone,
    DROP COLUMN IF EXISTS opened_date,
    DROP COLUMN IF EXISTS google_place_id,
    DROP COLUMN IF EXISTS tags,
    DROP COLUMN IF EXISTS amenities,
    DROP COLUMN IF EXISTS gallery,
    DROP COLUMN IF EXISTS location,
    DROP COLUMN IF EXISTS address,
    DROP COLUMN IF EXISTS title;

-- Note: Keeping the following essential columns:
-- - id, name, category, description, phone, website_url
-- - latitude, longitude, road_address, search_query
-- - rating, review_count, popularity
-- - opening_hours, naver_place_id
-- - keywords, keyword_vector, description_vector
-- - created_at, updated_at
