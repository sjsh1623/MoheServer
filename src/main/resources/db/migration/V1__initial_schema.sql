-- V1: Initial schema for Mohe Spring Application

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) UNIQUE NOT NULL,
    mbti VARCHAR(4),
    age_range VARCHAR(20),
    transportation_method VARCHAR(50),
    space_preferences TEXT[],
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create places table
CREATE TABLE IF NOT EXISTS places (
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
CREATE TABLE IF NOT EXISTS place_descriptions (
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
CREATE TABLE IF NOT EXISTS place_images (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    url TEXT NOT NULL,
    order_index INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_place_images_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create place_business_hours table
CREATE TABLE IF NOT EXISTS place_business_hours (
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
CREATE TABLE IF NOT EXISTS place_sns (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_place_sns_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create bookmarks table
CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_bookmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_place UNIQUE (user_id, place_id)
);

-- Create activities table
CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    place_id BIGINT,
    timestamp TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_activities_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_activities_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL
);

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create temp_users table (for OTP verification)
CREATE TABLE IF NOT EXISTS temp_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    otp VARCHAR(10) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_nickname ON users(nickname);

CREATE INDEX idx_places_name ON places(name);
CREATE INDEX idx_places_category ON places USING GIN(category);
CREATE INDEX idx_places_rating ON places(rating DESC);
CREATE INDEX idx_places_ready ON places(ready);
CREATE INDEX idx_places_location ON places(latitude, longitude);

CREATE INDEX idx_place_descriptions_place_id ON place_descriptions(place_id);
CREATE INDEX idx_place_images_place_id ON place_images(place_id);
CREATE INDEX idx_place_business_hours_place_id ON place_business_hours(place_id);
CREATE INDEX idx_place_sns_place_id ON place_sns(place_id);

CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_place_id ON bookmarks(place_id);
CREATE INDEX idx_bookmarks_created_at ON bookmarks(created_at DESC);

CREATE INDEX idx_activities_user_id ON activities(user_id);
CREATE INDEX idx_activities_timestamp ON activities(timestamp DESC);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add triggers
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_places_updated_at
    BEFORE UPDATE ON places
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_place_descriptions_updated_at
    BEFORE UPDATE ON place_descriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
