DO $$
DECLARE
    import_status_constraint text;
    auto_validation_constraint text;
BEGIN
    FOR import_status_constraint IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'questions'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%import_status%'
    LOOP
        EXECUTE format('ALTER TABLE questions DROP CONSTRAINT IF EXISTS %I', import_status_constraint);
    END LOOP;

    FOR auto_validation_constraint IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'questions'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%auto_validation_status%'
    LOOP
        EXECUTE format('ALTER TABLE questions DROP CONSTRAINT IF EXISTS %I', auto_validation_constraint);
    END LOOP;
END $$;

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS auto_validation_score integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auto_validation_status varchar(40) NOT NULL DEFAULT 'NEEDS_HUMAN_REVIEW',
    ADD COLUMN IF NOT EXISTS auto_validation_errors TEXT,
    ADD COLUMN IF NOT EXISTS auto_validation_warnings TEXT,
    ADD COLUMN IF NOT EXISTS auto_validated_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS broken_image_detected boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS suspicious_text_detected boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS requires_asset_review boolean NOT NULL DEFAULT false;

ALTER TABLE questions
    ALTER COLUMN auto_validation_status SET DEFAULT 'NEEDS_HUMAN_REVIEW';

ALTER TABLE questions
    ADD CONSTRAINT questions_import_status_check
        CHECK (import_status IN (
            'DRAFT',
            'NEEDS_REVIEW',
            'VALIDATED',
            'AUTO_VALIDATED',
            'PUBLISHED',
            'INVALID'
        )) NOT VALID;

ALTER TABLE questions
    ADD CONSTRAINT questions_auto_validation_status_check
        CHECK (auto_validation_status IN (
            'SAFE_TO_AUTO_VALIDATE',
            'NEEDS_HUMAN_REVIEW',
            'AUTO_INVALID'
        )) NOT VALID;

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
