ALTER TABLE official_exam_sources
    ADD COLUMN IF NOT EXISTS cached_answer_key_url VARCHAR(1000);
