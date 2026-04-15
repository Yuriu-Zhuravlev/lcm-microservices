CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    html_content TEXT,
    order_index INT DEFAULT 0,
    course_id BIGINT NOT NULL,
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    lesson_id BIGINT NOT NULL,
    CONSTRAINT fk_questions_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS question_options (
    question_id BIGINT NOT NULL,
    option_letter CHAR(1) NOT NULL,
    text TEXT NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (question_id, option_letter),
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lessons_course_id ON lessons(course_id);
CREATE INDEX IF NOT EXISTS idx_questions_lesson_id ON questions(lesson_id);
CREATE INDEX IF NOT EXISTS idx_options_question_id ON question_options(question_id);