CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(10)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS otp_config (
    id          INT PRIMARY KEY DEFAULT 1,
    code_length INT NOT NULL DEFAULT 6 CHECK (code_length BETWEEN 4 AND 10),
    ttl_seconds INT NOT NULL DEFAULT 300 CHECK (ttl_seconds > 0),
    CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO otp_config (id, code_length, ttl_seconds)
VALUES (1, 6, 300)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS otp_codes (
    id           BIGSERIAL PRIMARY KEY,
    operation_id VARCHAR(255) NOT NULL,
    code         VARCHAR(20)  NOT NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_lookup ON otp_codes(operation_id, user_id, status);
