CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(120) NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    username VARCHAR(60) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    bio VARCHAR(500),
    target_course VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(180) NOT NULL,
    statement TEXT NOT NULL,
    image_url VARCHAR(500),
    subject VARCHAR(80) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    subtopic VARCHAR(120),
    difficulty VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    exam VARCHAR(120) NOT NULL,
    competency VARCHAR(120),
    ability VARCHAR(120),
    explanation TEXT,
    correct_alternative VARCHAR(1) NOT NULL
);

CREATE TABLE alternatives (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    letter VARCHAR(1) NOT NULL,
    text TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    question_id BIGINT NOT NULL,
    CONSTRAINT fk_alternative_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE user_answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    chosen_alternative VARCHAR(1) NOT NULL,
    time_spent_seconds BIGINT NOT NULL,
    correct BOOLEAN NOT NULL,
    attempt_number INTEGER NOT NULL,
    CONSTRAINT fk_user_answers_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_answers_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE error_notebook (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    error_count INTEGER NOT NULL DEFAULT 1,
    last_reviewed_at DATE,
    next_review_at DATE,
    mastery_status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_error_notebook_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_error_notebook_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uk_error_notebook_user_question UNIQUE (user_id, question_id)
);

CREATE TABLE mock_exams (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(160) NOT NULL,
    user_id BIGINT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    finished BOOLEAN NOT NULL DEFAULT FALSE,
    final_score NUMERIC(5,2),
    CONSTRAINT fk_mock_exams_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE mock_exam_questions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    mock_exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INTEGER NOT NULL,
    CONSTRAINT fk_mock_exam_questions_mock_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_mock_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE favorite_questions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    CONSTRAINT fk_favorite_questions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_questions_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uk_favorite_questions_user_question UNIQUE (user_id, question_id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_questions_subject ON questions(subject);
CREATE INDEX idx_questions_topic ON questions(topic);
CREATE INDEX idx_questions_subtopic ON questions(subtopic);
CREATE INDEX idx_questions_year ON questions(year);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_user_answers_user_question ON user_answers(user_id, question_id);
CREATE INDEX idx_error_notebook_user ON error_notebook(user_id);
CREATE INDEX idx_mock_exams_user ON mock_exams(user_id);
