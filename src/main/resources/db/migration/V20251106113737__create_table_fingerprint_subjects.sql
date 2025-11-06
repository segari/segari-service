CREATE TABLE IF NOT EXISTS fingerprint_subjects
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    internal_tools_user_id BIGINT                   NOT NULL,
    template_group         VARCHAR                  NOT NULL,
    template               bytea                  NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_fingerprint_subjects_internal_tools_user_id
    ON fingerprint_subjects (internal_tools_user_id);