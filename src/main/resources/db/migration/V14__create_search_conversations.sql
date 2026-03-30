CREATE TABLE IF NOT EXISTS search_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    title VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_search_conv_user ON search_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_search_conv_session ON search_conversations(session_id);

CREATE TABLE IF NOT EXISTS search_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES search_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    place_ids BIGINT[],
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_search_msg_conv ON search_messages(conversation_id);
