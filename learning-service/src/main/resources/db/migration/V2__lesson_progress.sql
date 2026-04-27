CREATE TABLE IF NOT EXISTS user_lesson_progress (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    enrolment_id BIGINT NOT NULL,
                                                    lesson_id BIGINT NOT NULL,
                                                    total_questions INTEGER NOT NULL DEFAULT 0,
                                                    correct_answers INTEGER NOT NULL DEFAULT 0,
                                                    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
                                                    completed_at TIMESTAMP,
                                                    CONSTRAINT fk_enrolment FOREIGN KEY (enrolment_id) REFERENCES enrolments(id),
    CONSTRAINT unique_enrolment_lesson UNIQUE (enrolment_id, lesson_id)
    );

CREATE INDEX IF NOT EXISTS idx_lesson_progress_enrolment ON user_lesson_progress(enrolment_id);