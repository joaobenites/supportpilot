CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Emails are compared case-insensitively: the app stores them lowercased and this index enforces it.
CREATE UNIQUE INDEX users_email_uk ON users (lower(email));
