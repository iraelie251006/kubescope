ALTER TABLE app_user
    ADD COLUMN name                   VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN account_non_expired    BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN account_non_locked     BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN credentials_non_expired BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN enabled                BOOLEAN      NOT NULL DEFAULT TRUE;

ALTER TABLE app_user
    ALTER COLUMN name DROP DEFAULT;