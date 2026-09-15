-- =============================================
-- Our Photobooth Memories - Initial Schema
-- =============================================

-- Users table (only 2 owner accounts, seeded)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'OWNER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Photo sessions (buổi chụp)
CREATE TABLE photo_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    session_date DATE NOT NULL,
    location VARCHAR(255),
    mood_tag VARCHAR(100),
    spotify_link VARCHAR(500),
    description TEXT,
    cover_photo_url VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    video_url VARCHAR(500),
    video_thumbnail_url VARCHAR(500),
    video_public_id VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Photos
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES photo_sessions(id) ON DELETE CASCADE,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    original_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500) NOT NULL,
    caption TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Love notes on photos
CREATE TABLE love_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    written_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Reactions on photos
CREATE TABLE reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    emoji VARCHAR(10) NOT NULL,
    reacted_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reaction_per_user UNIQUE(photo_id, emoji, reacted_by)
);

-- Milestones (countdown targets)
CREATE TABLE milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    target_date DATE NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Couple configuration (anniversary date, couple name, etc.)
CREATE TABLE couple_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============
-- Indexes
-- =============
CREATE INDEX idx_sessions_date ON photo_sessions(session_date DESC);
CREATE INDEX idx_sessions_public ON photo_sessions(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_sessions_created_by ON photo_sessions(created_by);
CREATE INDEX idx_photos_session ON photos(session_id);
CREATE INDEX idx_photos_public ON photos(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_photos_sort ON photos(session_id, sort_order);
CREATE INDEX idx_reactions_photo ON reactions(photo_id);
CREATE INDEX idx_notes_photo ON love_notes(photo_id);
CREATE INDEX idx_milestones_date ON milestones(target_date ASC);
