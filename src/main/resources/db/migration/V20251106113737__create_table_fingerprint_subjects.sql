CREATE TABLE IF NOT EXISTS fingerprint_subjects
(
    id                     BIGINT PRIMARY KEY,
    internal_tools_user_id BIGINT  NOT NULL,
    template_group         VARCHAR NOT NULL,
    template_vendor        VARCHAR NOT NULL,
    template               VARBINARY NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fingerprint_subjects_internal_tools_user_id
    ON fingerprint_subjects (internal_tools_user_id);