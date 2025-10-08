
-- V6__update_place_schema.sql

-- 1. Drop existing foreign key constraints if they exist
ALTER TABLE IF EXISTS place_descriptions DROP CONSTRAINT IF EXISTS fk_place_descriptions_place_id;
ALTER TABLE IF EXISTS place_images DROP CONSTRAINT IF EXISTS fk_place_images_place_id;
ALTER TABLE IF EXISTS place_business_hours DROP CONSTRAINT IF EXISTS fk_place_business_hours_place_id;
ALTER TABLE IF EXISTS place_sns DROP CONSTRAINT IF EXISTS fk_place_sns_place_id;

-- 2. Drop new tables if they already exist
DROP TABLE IF EXISTS place_sns;
DROP TABLE IF EXISTS place_business_hours;
DROP TABLE IF EXISTS place_images;
DROP TABLE IF EXISTS place_descriptions;

-- 3. Alter 'places' table
ALTER TABLE places
    DROP COLUMN IF EXISTS opening_hours,
    ADD COLUMN IF NOT EXISTS website_url TEXT,
    ADD COLUMN IF NOT EXISTS rating NUMERIC,
    ADD COLUMN IF NOT EXISTS review_count INTEGER,
    ADD COLUMN IF NOT EXISTS category VARCHAR(255)[],
    ADD COLUMN IF NOT EXISTS keyword VARCHAR(255)[],
    ADD COLUMN IF NOT EXISTS keyword_vector TEXT,
    ADD COLUMN IF NOT EXISTS parking_available BOOLEAN,
    ADD COLUMN IF NOT EXISTS pet_friendly BOOLEAN,
    ADD COLUMN IF NOT EXISTS ready BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Add a function to update the 'updated_at' column
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if it exists and create a new one
DROP TRIGGER IF EXISTS set_timestamp ON places;
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON places
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();

-- 4. Create new tables
CREATE TABLE place_descriptions (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    original_description TEXT,
    ai_summary TEXT,
    ollama_description TEXT,
    search_query VARCHAR(500),
    updated_at TIMESTAMP
);

CREATE TABLE place_images (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    url TEXT,
    order_index INTEGER,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE place_business_hours (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    day_of_week VARCHAR(10),
    open TIME,
    close TIME,
    description TEXT,
    is_operating BOOLEAN,
    last_order_minutes INTEGER
);

CREATE TABLE place_sns (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    platform VARCHAR(50),
    url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 5. Add foreign key constraints
ALTER TABLE place_descriptions ADD CONSTRAINT fk_place_descriptions_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
ALTER TABLE place_images ADD CONSTRAINT fk_place_images_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
ALTER TABLE place_business_hours ADD CONSTRAINT fk_place_business_hours_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
ALTER TABLE place_sns ADD CONSTRAINT fk_place_sns_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
