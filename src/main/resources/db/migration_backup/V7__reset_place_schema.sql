-- V7: Complete schema reset to match current entities
-- Drop all place-related foreign key constraints first
ALTER TABLE IF EXISTS bookmarks DROP CONSTRAINT IF EXISTS bookmarks_place_id_fkey;
ALTER TABLE IF EXISTS place_external_raw DROP CONSTRAINT IF EXISTS fk2bljf14lvmunwaqx9sekfwhvo;
ALTER TABLE IF EXISTS place_similarity DROP CONSTRAINT IF EXISTS fk2m4uwpo1ldcjbes8npi76w380;
ALTER TABLE IF EXISTS place_similarity DROP CONSTRAINT IF EXISTS fk9rl91kwdphr1fc0ko61di9dwe;
ALTER TABLE IF EXISTS place_mbti_descriptions DROP CONSTRAINT IF EXISTS fk5vxvflcmlkvc5kjnpa39wb2so;
ALTER TABLE IF EXISTS place_description_vectors DROP CONSTRAINT IF EXISTS fk7ijmn4nkgrsbeaayb6br7slb9;
ALTER TABLE IF EXISTS place_images DROP CONSTRAINT IF EXISTS fk_place_images_place_id;
ALTER TABLE IF EXISTS place_similarity_topk DROP CONSTRAINT IF EXISTS fkgcvmr7rkg7x3ln5djthgk3ay4;
ALTER TABLE IF EXISTS place_similarity_topk DROP CONSTRAINT IF EXISTS fkr0kaakmm2s803ltga7pmx3lfe;
ALTER TABLE IF EXISTS vector_similarities DROP CONSTRAINT IF EXISTS fkig95fclqlt68h03iov7d5dmul;
ALTER TABLE IF EXISTS recent_views DROP CONSTRAINT IF EXISTS fkq35f4cw8ycj7fdamxhsbexia9;
ALTER TABLE IF EXISTS prompts DROP CONSTRAINT IF EXISTS fksiu4e8d9m53c33flsk66vwhfk;
ALTER TABLE IF EXISTS place_descriptions DROP CONSTRAINT IF EXISTS fk_place_descriptions_place_id;
ALTER TABLE IF EXISTS place_business_hours DROP CONSTRAINT IF EXISTS fk_place_business_hours_place_id;
ALTER TABLE IF EXISTS place_sns DROP CONSTRAINT IF EXISTS fk_place_sns_place_id;

-- Drop all place-related tables
DROP TABLE IF EXISTS place_descriptions CASCADE;
DROP TABLE IF EXISTS place_images CASCADE;
DROP TABLE IF EXISTS place_business_hours CASCADE;
DROP TABLE IF EXISTS place_sns CASCADE;
DROP TABLE IF EXISTS places CASCADE;

-- Recreate places table with correct schema
CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    road_address TEXT,
    website_url TEXT,
    rating NUMERIC(3,2) DEFAULT 0.0,
    review_count INTEGER DEFAULT 0,
    category VARCHAR(100)[],
    keyword VARCHAR(255)[],
    keyword_vector TEXT,
    opening_hours JSONB,
    parking_available BOOLEAN DEFAULT FALSE,
    pet_friendly BOOLEAN DEFAULT FALSE,
    ready BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create place_descriptions table
CREATE TABLE place_descriptions (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    original_description TEXT,
    ai_summary TEXT,
    ollama_description TEXT,
    search_query VARCHAR(500),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_place_descriptions_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create place_images table
CREATE TABLE place_images (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    url TEXT NOT NULL,
    order_index INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_place_images_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create place_business_hours table
CREATE TABLE place_business_hours (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    day_of_week VARCHAR(10),
    open TIME,
    close TIME,
    description TEXT,
    is_operating BOOLEAN DEFAULT TRUE,
    last_order_minutes INTEGER,
    CONSTRAINT fk_place_business_hours_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create place_sns table
CREATE TABLE place_sns (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_place_sns_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_places_name ON places(name);
CREATE INDEX idx_places_category ON places USING GIN(category);
CREATE INDEX idx_places_rating ON places(rating DESC);
CREATE INDEX idx_places_ready ON places(ready);
CREATE INDEX idx_places_location ON places(latitude, longitude);
CREATE INDEX idx_place_descriptions_place_id ON place_descriptions(place_id);
CREATE INDEX idx_place_images_place_id ON place_images(place_id);
CREATE INDEX idx_place_business_hours_place_id ON place_business_hours(place_id);
CREATE INDEX idx_place_sns_place_id ON place_sns(place_id);

-- Create updated_at trigger function if not exists
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add triggers
CREATE TRIGGER trigger_places_updated_at
    BEFORE UPDATE ON places
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_place_descriptions_updated_at
    BEFORE UPDATE ON place_descriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Recreate bookmarks foreign key if bookmarks table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'bookmarks') THEN
        ALTER TABLE bookmarks ADD CONSTRAINT bookmarks_place_id_fkey
            FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Recreate other foreign keys if tables exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'place_mbti_descriptions') THEN
        ALTER TABLE place_mbti_descriptions ADD CONSTRAINT fk5vxvflcmlkvc5kjnpa39wb2so
            FOREIGN KEY (place_id) REFERENCES places(id);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'place_description_vectors') THEN
        ALTER TABLE place_description_vectors ADD CONSTRAINT fk7ijmn4nkgrsbeaayb6br7slb9
            FOREIGN KEY (place_id) REFERENCES places(id);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recent_views') THEN
        ALTER TABLE recent_views ADD CONSTRAINT fkq35f4cw8ycj7fdamxhsbexia9
            FOREIGN KEY (place_id) REFERENCES places(id);
    END IF;
END $$;
