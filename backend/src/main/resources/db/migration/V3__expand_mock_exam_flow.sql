ALTER TABLE mock_exams
    ADD COLUMN finished_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN time_spent_seconds BIGINT;

CREATE TABLE mock_exam_answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    mock_exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    chosen_alternative VARCHAR(1) NOT NULL,
    time_spent_seconds BIGINT NOT NULL,
    correct BOOLEAN NOT NULL,
    CONSTRAINT fk_mock_exam_answers_mock_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_mock_exam_answers_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uk_mock_exam_answers_exam_question UNIQUE (mock_exam_id, question_id)
);

CREATE INDEX idx_mock_exam_answers_exam ON mock_exam_answers(mock_exam_id);
