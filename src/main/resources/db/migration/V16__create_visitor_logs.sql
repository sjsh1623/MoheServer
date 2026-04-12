CREATE TABLE IF NOT EXISTS visitor_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_type VARCHAR(20),
    os VARCHAR(50),
    browser VARCHAR(50),
    page_path VARCHAR(500),
    referrer TEXT,
    user_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visitor_logs_created_at ON visitor_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_visitor_logs_session_id ON visitor_logs (session_id);
CREATE INDEX IF NOT EXISTS idx_visitor_logs_page_path ON visitor_logs (page_path);
