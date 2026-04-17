CREATE TABLE IF NOT EXISTS enrolments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    progress DOUBLE PRECISION DEFAULT 0.0,
    -- Гарантуємо, що користувач не запишеться на один курс двічі
    CONSTRAINT unique_user_course UNIQUE (user_id, course_id)
    );

-- Індекси для швидкого пошуку курсів користувача
CREATE INDEX idx_enrolment_user ON enrolments(user_id);
-- Індекс для аналітики (скільки людей на курсі)
CREATE INDEX idx_enrolment_course ON enrolments(course_id);