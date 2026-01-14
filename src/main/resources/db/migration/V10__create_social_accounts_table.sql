-- Create social_accounts table for OAuth social login
CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    provider_name VARCHAR(255),
    access_token VARCHAR(2048),
    refresh_token VARCHAR(2048),
    token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(provider, provider_id)
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_social_accounts_user_id ON social_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_social_accounts_provider_id ON social_accounts(provider, provider_id);
CREATE INDEX IF NOT EXISTS idx_social_accounts_provider_email ON social_accounts(provider, provider_email);
