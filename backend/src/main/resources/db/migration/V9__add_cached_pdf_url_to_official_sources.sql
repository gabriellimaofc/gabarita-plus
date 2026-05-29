ALTER TABLE official_exam_sources
    ADD COLUMN IF NOT EXISTS cached_pdf_url VARCHAR(1000);
