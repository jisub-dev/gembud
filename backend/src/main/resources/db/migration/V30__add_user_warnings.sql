CREATE TABLE user_warnings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    report_id BIGINT NOT NULL REFERENCES reports(id),
    admin_user_id BIGINT NOT NULL REFERENCES users(id),
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_user_warnings_report_id ON user_warnings(report_id);
CREATE INDEX idx_user_warnings_user_id_created_at
    ON user_warnings(user_id, created_at DESC);
