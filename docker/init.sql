-- Database initialization script for Islamophobia Detector

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create indexes for performance (will be created by JPA but explicit for clarity)
CREATE INDEX IF NOT EXISTS idx_content_items_violation ON content_items(is_violation);
CREATE INDEX IF NOT EXISTS idx_content_items_collected_at ON content_items(collected_at);
CREATE INDEX IF NOT EXISTS idx_content_analysis_confirmed ON content_analysis(is_confirmed_violation);

-- Grant permissions (adjust as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

COMMENT ON DATABASE islamophobia_detector IS 'Database for Islamophobia Content Detection System';
