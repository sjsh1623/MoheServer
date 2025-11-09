-- V5: Create comments table for user place reviews
-- 사용자가 장소에 작성하는 댓글 테이블 생성

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    rating DECIMAL(2, 1),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating_range CHECK (rating IS NULL OR (rating >= 1.0 AND rating <= 5.0))
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_comments_place_id ON comments(place_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at DESC);
