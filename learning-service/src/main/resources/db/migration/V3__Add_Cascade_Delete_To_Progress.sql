ALTER TABLE user_lesson_progress
DROP CONSTRAINT IF EXISTS fk_enrolment;

ALTER TABLE user_lesson_progress
    ADD CONSTRAINT fk_enrolment
        FOREIGN KEY (enrolment_id)
            REFERENCES enrolments (id)
            ON DELETE CASCADE;