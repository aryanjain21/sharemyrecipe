-- V1: Initial schema (same as API - worker validates only)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    handle        VARCHAR(100) NOT NULL UNIQUE,
    display_name  VARCHAR(255) NOT NULL,
    bio           TEXT,
    avatar_url    VARCHAR(500),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'CHEF', 'ADMIN')),
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE TABLE recipes (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(255) NOT NULL,
    summary       TEXT,
    ingredients   TEXT        NOT NULL,
    steps         TEXT        NOT NULL,
    labels        TEXT[],
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recipes_fts ON recipes
    USING gin(to_tsvector('english',
        coalesce(title,'') || ' ' ||
        coalesce(summary,'') || ' ' ||
        coalesce(ingredients,'') || ' ' ||
        coalesce(steps,'')
    ));

CREATE INDEX idx_recipes_chef_id       ON recipes(chef_id);
CREATE INDEX idx_recipes_status        ON recipes(status);
CREATE INDEX idx_recipes_published_at  ON recipes(published_at);

CREATE TABLE recipe_images (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id   UUID        NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    original_url  VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    display_order SMALLINT   NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
