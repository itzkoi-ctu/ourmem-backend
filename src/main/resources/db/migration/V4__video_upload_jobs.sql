CREATE TABLE video_uploads (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES photo_sessions(id) ON DELETE CASCADE,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'READY', 'FAILED')),
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX video_uploads_session_created ON video_uploads (session_id, created_at DESC);
CREATE UNIQUE INDEX video_uploads_one_active ON video_uploads (session_id) WHERE status = 'PROCESSING';
-- Access is through the authenticated backend, never through the Supabase public API.
ALTER TABLE video_uploads ENABLE ROW LEVEL SECURITY;
