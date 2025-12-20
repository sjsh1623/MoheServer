-- Create place_menus table for storing menu information
-- This table stores menu data crawled from Naver for each place

CREATE TABLE IF NOT EXISTS place_menus (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    price VARCHAR(50),
    description VARCHAR(1000),
    image_url VARCHAR(2048),
    image_path VARCHAR(2048),
    is_popular BOOLEAN DEFAULT false,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_place_menus_place_id ON place_menus(place_id);
CREATE INDEX IF NOT EXISTS idx_place_menus_is_popular ON place_menus(place_id, is_popular);
CREATE INDEX IF NOT EXISTS idx_place_menus_display_order ON place_menus(place_id, display_order);

-- Add comment for documentation
COMMENT ON TABLE place_menus IS 'Stores menu information for places, crawled from Naver';
COMMENT ON COLUMN place_menus.name IS 'Menu item name';
COMMENT ON COLUMN place_menus.price IS 'Price as displayed (e.g., "10,000원", "시가")';
COMMENT ON COLUMN place_menus.description IS 'Menu item description';
COMMENT ON COLUMN place_menus.image_url IS 'Original image URL from crawler';
COMMENT ON COLUMN place_menus.image_path IS 'Saved image path (e.g., /images/menu/123_menuName_uuid.jpg)';
COMMENT ON COLUMN place_menus.is_popular IS 'Whether this is a popular/recommended menu item';
COMMENT ON COLUMN place_menus.display_order IS 'Order for display purposes';
