CREATE TABLE IF NOT EXISTS enrolments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    total_lessons_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT unique_user_course UNIQUE (user_id, course_id)
    );

CREATE INDEX IF NOT EXISTS idx_enrolment_user ON enrolments(user_id);
CREATE INDEX IF NOT EXISTS idx_enrolment_course ON enrolments(course_id);