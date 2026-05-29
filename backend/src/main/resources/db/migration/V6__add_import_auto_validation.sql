ALTER TYPE question_import_status ADD VALUE IF NOT EXISTS 'AUTO_VALIDATED';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auto_validation_status') THEN
        CREATE TYPE auto_validation_status AS ENUM (
            'SAFE_TO_AUTO_VALIDATE',
            'NEEDS_HUMAN_REVIEW',
            'AUTO_INVALID'
        );
    END IF;
END $$;

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS auto_validation_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auto_validation_status auto_validation_status NOT NULL DEFAULT 'NEEDS_HUMAN_REVIEW',
    ADD COLUMN IF NOT EXISTS auto_validation_errors TEXT,
    ADD COLUMN IF NOT EXISTS auto_validation_warnings TEXT,
    ADD COLUMN IF NOT EXISTS auto_validated_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS broken_image_detected boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS suspicious_text_detected boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS requires_asset_review boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS official_exam_sources (
    id BIGSERIAL PRIMARY KEY,
    exam varchar(120) NOT NULL,
    year integer NOT NULL,
    exam_day integer,
    book_color varchar(40),
    pdf_url varchar(1000) NOT NULL,
    answer_key_url varchar(1000),
    source_url varchar(1000) NOT NULL,
    local_pdf_path varchar(1000),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_questions_auto_validation_status
    ON questions(auto_validation_status);

CREATE INDEX IF NOT EXISTS idx_questions_auto_validation_flags
    ON questions(broken_image_detected, suspicious_text_detected, requires_asset_review);

CREATE INDEX IF NOT EXISTS idx_official_exam_sources_lookup
    ON official_exam_sources(exam, year, exam_day, book_color);
