ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS source VARCHAR(80),
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS source_exam VARCHAR(120),
    ADD COLUMN IF NOT EXISTS source_year INTEGER,
    ADD COLUMN IF NOT EXISTS source_question_number INTEGER,
    ADD COLUMN IF NOT EXISTS source_book_color VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_day INTEGER,
    ADD COLUMN IF NOT EXISTS source_page INTEGER,
    ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS import_batch_id BIGINT,
    ADD COLUMN IF NOT EXISTS statement_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS statement_html TEXT,
    ADD COLUMN IF NOT EXISTS import_status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED';

ALTER TABLE questions
    ALTER COLUMN source SET DEFAULT 'PLATFORM',
    ALTER COLUMN source_url SET DEFAULT 'internal://legacy-question',
    ALTER COLUMN import_status SET DEFAULT 'PUBLISHED';

UPDATE questions
SET source = COALESCE(source, 'PLATFORM'),
    source_url = COALESCE(source_url, 'internal://legacy-question'),
    source_exam = COALESCE(source_exam, exam),
    source_year = COALESCE(source_year, year),
    statement_hash = COALESCE(statement_hash, md5(coalesce(statement, ''))),
    import_status = COALESCE(import_status, 'PUBLISHED'),
    imported_at = COALESCE(imported_at, created_at)
WHERE source IS NULL
   OR source_url IS NULL
   OR source_exam IS NULL
   OR source_year IS NULL
   OR statement_hash IS NULL
   OR import_status IS NULL
   OR imported_at IS NULL;

ALTER TABLE questions
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN source_url SET NOT NULL,
    ALTER COLUMN source_exam SET NOT NULL,
    ALTER COLUMN source_year SET NOT NULL,
    ALTER COLUMN statement_hash SET NOT NULL;

ALTER TABLE alternatives
    ADD COLUMN IF NOT EXISTS html TEXT;

CREATE TABLE IF NOT EXISTS import_batches (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    source_name VARCHAR(120) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    exam VARCHAR(120) NOT NULL,
    year INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_items INTEGER NOT NULL DEFAULT 0,
    imported_items INTEGER NOT NULL DEFAULT 0,
    skipped_items INTEGER NOT NULL DEFAULT 0,
    failed_items INTEGER NOT NULL DEFAULT 0,
    needs_review_items INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    error_report TEXT
);

ALTER TABLE questions
    ADD CONSTRAINT fk_questions_import_batch
        FOREIGN KEY (import_batch_id) REFERENCES import_batches(id);

CREATE TABLE IF NOT EXISTS question_assets (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    question_id BIGINT NOT NULL,
    alternative_id BIGINT,
    type VARCHAR(30) NOT NULL,
    url VARCHAR(1000),
    storage_path VARCHAR(1000),
    original_file_name VARCHAR(255),
    source_page INTEGER,
    crop_x INTEGER,
    crop_y INTEGER,
    crop_width INTEGER,
    crop_height INTEGER,
    alt_text VARCHAR(500),
    caption VARCHAR(500),
    checksum VARCHAR(128),
    CONSTRAINT fk_question_assets_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_question_assets_alternative FOREIGN KEY (alternative_id) REFERENCES alternatives(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_questions_import_status ON questions(import_status);
CREATE INDEX IF NOT EXISTS idx_questions_statement_hash ON questions(statement_hash);
CREATE INDEX IF NOT EXISTS idx_questions_source_identity
    ON questions(source_exam, source_year, source_question_number, source_day, source_book_color);
CREATE INDEX IF NOT EXISTS idx_questions_import_batch_id ON questions(import_batch_id);
CREATE INDEX IF NOT EXISTS idx_import_batches_status ON import_batches(status);
CREATE INDEX IF NOT EXISTS idx_question_assets_question_id ON question_assets(question_id);
CREATE INDEX IF NOT EXISTS idx_question_assets_alternative_id ON question_assets(alternative_id);
