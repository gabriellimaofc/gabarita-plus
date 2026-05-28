ALTER TABLE questions
    ADD COLUMN official_source_url VARCHAR(1000),
    ADD COLUMN official_pdf_url VARCHAR(1000),
    ADD COLUMN official_answer_key_url VARCHAR(1000),
    ADD COLUMN official_page INTEGER,
    ADD COLUMN validated_against_official_source BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN validated_at TIMESTAMPTZ,
    ADD COLUMN external_provider VARCHAR(120),
    ADD COLUMN external_provider_url VARCHAR(1000),
    ADD COLUMN external_question_id VARCHAR(255),
    ADD COLUMN external_license VARCHAR(255);

UPDATE questions
SET validated_against_official_source = FALSE
WHERE validated_against_official_source IS NULL;

CREATE INDEX IF NOT EXISTS idx_questions_external_provider_question_id
    ON questions (external_provider, external_question_id);

CREATE INDEX IF NOT EXISTS idx_questions_validated_against_official_source
    ON questions (validated_against_official_source);
