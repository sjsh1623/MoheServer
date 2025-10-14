-- Create place_reviews table
CREATE TABLE IF NOT EXISTS place_reviews (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    review_text TEXT,
    order_index INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_place_reviews_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_place_reviews_place_id ON place_reviews(place_id);
CREATE INDEX IF NOT EXISTS idx_place_reviews_order_index ON place_reviews(place_id, order_index);
