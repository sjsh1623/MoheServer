-- Migration: Add search_query column to places table
-- Version: V4
-- Description: Add search_query field to store the location + category query used for Naver API search
-- Author: Claude Code
-- Date: 2025-10-07

-- Add search_query column to places table
ALTER TABLE places
ADD COLUMN IF NOT EXISTS search_query VARCHAR(500);

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_places_search_query ON places(search_query);

-- Add comment for documentation
COMMENT ON COLUMN places.search_query IS 'The search query (location + category) used to find this place via Naver API';
