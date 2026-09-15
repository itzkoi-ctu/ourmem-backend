-- =============================================
-- Seed 2 fixed owner accounts
-- Default password: 'ourmemory2024' (BCrypt encoded)
-- CHANGE THESE PASSWORDS after first login!
-- =============================================

INSERT INTO users (email, password, display_name, role) VALUES
('anh@ourmemory.app', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Anh', 'OWNER'),
('em@ourmemory.app', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Em', 'OWNER');

-- Seed default couple config
INSERT INTO couple_config (config_key, config_value) VALUES
('anniversary_date', '2026-08-01'),
('couple_name', 'Our Photobooth Memories');
