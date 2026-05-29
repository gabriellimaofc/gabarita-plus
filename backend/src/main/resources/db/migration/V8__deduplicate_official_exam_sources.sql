DELETE FROM official_exam_sources target
USING (
    SELECT id,
           row_number() OVER (
               PARTITION BY lower(exam), year, coalesce(exam_day, -1), upper(coalesce(book_color, ''))
               ORDER BY updated_at DESC, id DESC
           ) AS duplicate_rank
    FROM official_exam_sources
) ranked
WHERE target.id = ranked.id
  AND ranked.duplicate_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS idx_official_exam_sources_unique_identity
    ON official_exam_sources (
        lower(exam),
        year,
        coalesce(exam_day, -1),
        upper(coalesce(book_color, ''))
    );
