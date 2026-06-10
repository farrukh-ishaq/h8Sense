-- Database initialization script for Islamophobia Detector

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the content_items table with proper column definitions
CREATE TABLE IF NOT EXISTS content_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    platform VARCHAR(100) NOT NULL,
    content TEXT,
    title VARCHAR(500),
    author VARCHAR(255),
    url VARCHAR(2048),
    published_at TIMESTAMP NOT NULL,
    collected_at TIMESTAMP NOT NULL,
    is_violation BOOLEAN NOT NULL DEFAULT FALSE,
    violation_confidence DOUBLE PRECISION,
    violation_category VARCHAR(100),
    likes INTEGER,
    shares INTEGER,
    comments INTEGER
);

-- Create the content_analysis table
CREATE TABLE IF NOT EXISTS content_analysis (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_item_id UUID UNIQUE NOT NULL REFERENCES content_items(id),
    is_confirmed_violation BOOLEAN NOT NULL,
    violation_explanation TEXT,
    counter_argument TEXT,
    llm_raw_response TEXT,
    model_used VARCHAR(100),
    tokens_used INTEGER,
    analyzed_at TIMESTAMP NOT NULL
);

-- Create the quran_references collection table
CREATE TABLE IF NOT EXISTS quran_references (
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    quran_reference VARCHAR(255)
);

-- Create the hadith_references collection table
CREATE TABLE IF NOT EXISTS hadith_references (
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    hadith_reference VARCHAR(500)
);

-- Create the user_feedback table
CREATE TABLE IF NOT EXISTS user_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    feedback_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    user_agent VARCHAR(500),
    user_id VARCHAR(100),
    comment TEXT,
    created_at TIMESTAMP NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create the feedback_tracking table
CREATE TABLE IF NOT EXISTS feedback_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ip_address VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(255),
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    feedback_count INTEGER NOT NULL DEFAULT 0,
    last_feedback_at TIMESTAMP NOT NULL,
    first_feedback_at TIMESTAMP NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason VARCHAR(500)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_content_source ON content_items(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_content_timestamp ON content_items(collected_at);
CREATE INDEX IF NOT EXISTS idx_content_violation ON content_items(is_violation);
CREATE INDEX IF NOT EXISTS idx_analysis_content ON content_analysis(content_item_id);
CREATE INDEX IF NOT EXISTS idx_analysis_timestamp ON content_analysis(analyzed_at);
CREATE INDEX IF NOT EXISTS idx_feedback_analysis ON user_feedback(analysis_id);
CREATE INDEX IF NOT EXISTS idx_feedback_ip ON user_feedback(ip_address);
CREATE INDEX IF NOT EXISTS idx_feedback_device ON user_feedback(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_feedback_timestamp ON user_feedback(created_at);
CREATE INDEX IF NOT EXISTS idx_tracking_ip ON feedback_tracking(ip_address);
CREATE INDEX IF NOT EXISTS idx_tracking_device ON feedback_tracking(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_tracking_analysis ON feedback_tracking(analysis_id);

-- Comment on database
COMMENT ON DATABASE islamophobia_detector IS 'Database for Islamophobia Content Detection System';
