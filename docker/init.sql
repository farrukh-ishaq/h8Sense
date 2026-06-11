-- Database initialization script for Islamophobia Detector

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS content_item (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    title VARCHAR(500),
    source_platform VARCHAR(100),
    source VARCHAR(500),
    source_url VARCHAR(500),
    source_type VARCHAR(50),
    author VARCHAR(255),
    predicted_category VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS content_metadata (
    content_item_id UUID NOT NULL REFERENCES content_item(id),
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS content_analysis (
    id UUID PRIMARY KEY,
    content_item_id UUID UNIQUE NOT NULL,
    is_confirmed_violation BOOLEAN NOT NULL,
    category VARCHAR(50),
    violation_confidence DECIMAL(4,3) NOT NULL DEFAULT 0.000,
    violation_explanation TEXT,
    counter_argument TEXT,
    llm_raw_response TEXT,
    model_used VARCHAR(100),
    tokens_used INTEGER,
    analyzed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_content_analysis_content_item
        FOREIGN KEY (content_item_id) REFERENCES content_item(id)
);

CREATE TABLE IF NOT EXISTS quran_references (
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    quran_references VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS hadith_references (
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    hadith_references VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS user_feedback (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    feedback_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    user_agent VARCHAR(500),
    user_id VARCHAR(100),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS feedback_tracking (
    id UUID PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(255),
    analysis_id UUID NOT NULL REFERENCES content_analysis(id),
    feedback_count INTEGER NOT NULL DEFAULT 0,
    last_feedback_at TIMESTAMP WITH TIME ZONE NOT NULL,
    first_feedback_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason VARCHAR(500)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_content_source_platform ON content_item(source_platform);
CREATE INDEX IF NOT EXISTS idx_content_timestamp ON content_item(created_at);
CREATE INDEX IF NOT EXISTS idx_content_type ON content_item(source_type);
CREATE INDEX IF NOT EXISTS idx_content_source_url ON content_item(source_url);
CREATE INDEX IF NOT EXISTS idx_analysis_content ON content_analysis(content_item_id);
CREATE INDEX IF NOT EXISTS idx_analysis_timestamp ON content_analysis(analyzed_at);
CREATE INDEX IF NOT EXISTS idx_feedback_analysis ON user_feedback(analysis_id);
CREATE INDEX IF NOT EXISTS idx_feedback_ip ON user_feedback(ip_address);
CREATE INDEX IF NOT EXISTS idx_feedback_device ON user_feedback(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_feedback_timestamp ON user_feedback(created_at);
CREATE INDEX IF NOT EXISTS idx_tracking_ip ON feedback_tracking(ip_address);
CREATE INDEX IF NOT EXISTS idx_tracking_device ON feedback_tracking(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_tracking_analysis ON feedback_tracking(analysis_id);

