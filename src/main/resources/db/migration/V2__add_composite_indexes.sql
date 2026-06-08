-- Composite index for unique visitor count queries
CREATE INDEX idx_click_analytics_url_ip ON click_analytics(url_id, ip_address);

-- Composite index for time-range click queries per URL
CREATE INDEX idx_click_analytics_url_clicked_at ON click_analytics(url_id, clicked_at);

-- Index on refresh token expiration for cleanup queries
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
