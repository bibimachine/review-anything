-- Review Anything 数据库表结构
-- 各平台共用此 Schema（SQLite）

CREATE TABLE IF NOT EXISTS configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    api_base_url TEXT,
    api_key TEXT,
    model_name TEXT DEFAULT 'deepseek-v4-pro',
    daily_review_count INTEGER DEFAULT 10,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    is_system INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    section TEXT NOT NULL,
    content TEXT NOT NULL,
    content_hash TEXT DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chunks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash TEXT DEFAULT '',
    heading_path TEXT DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chunk_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    is_hard INTEGER DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    next_review_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_reviewed_at DATETIME,
    llm_failed INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chunk_id) REFERENCES chunks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS checkins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    checkin_date TEXT NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_notes_section ON notes(section);
CREATE INDEX IF NOT EXISTS idx_chunks_note_id ON chunks(note_id);
CREATE INDEX IF NOT EXISTS idx_review_items_chunk_id ON review_items(chunk_id);
CREATE INDEX IF NOT EXISTS idx_review_items_next_review ON review_items(next_review_at);
