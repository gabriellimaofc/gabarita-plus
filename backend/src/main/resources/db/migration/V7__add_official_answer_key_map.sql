ALTER TABLE official_exam_sources
    ADD COLUMN IF NOT EXISTS answer_key_map_json TEXT;
