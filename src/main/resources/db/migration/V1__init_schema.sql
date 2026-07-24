-- Включение расширения для генерации UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Таблица country
CREATE TABLE IF NOT EXISTS country (
                                       id BIGSERIAL PRIMARY KEY,
                                       name VARCHAR(100) NOT NULL UNIQUE,
                                       code CHAR(2) NOT NULL UNIQUE,
                                       phone_code VARCHAR(10) NOT NULL
);

-- Таблица region
CREATE TABLE IF NOT EXISTS region (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(100) NOT NULL,
                                      country_id BIGINT NOT NULL REFERENCES country(id) ON DELETE CASCADE,
                                      auto_code VARCHAR(5) UNIQUE
);

-- Таблица city
CREATE TABLE IF NOT EXISTS city (
                                    id BIGSERIAL PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL,
                                    region_id BIGINT NOT NULL REFERENCES region(id) ON DELETE CASCADE
);

-- Таблица rinks
CREATE TABLE IF NOT EXISTS rinks (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(200) NOT NULL,
                                     country_id BIGINT NOT NULL REFERENCES country(id),
                                     region_id BIGINT NOT NULL REFERENCES region(id),
                                     city_id BIGINT NOT NULL REFERENCES city(id),
                                     address TEXT NOT NULL,
                                     phone VARCHAR(50),
                                     rating DECIMAL(3,1),
                                     latitude DECIMAL(10,8) NOT NULL,
                                     longitude DECIMAL(11,8) NOT NULL,
                                     features TEXT[]
);

-- Таблица users
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     phone VARCHAR(20) NOT NULL UNIQUE,
                                     password_hash VARCHAR(100) NOT NULL,
                                     role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
                                     subrole VARCHAR(20) CHECK (subrole IN ('PLAYER', 'SPECIALIST')),
                                     status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
                                     registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     last_login_at TIMESTAMP
);

-- Таблица profiles
CREATE TABLE IF NOT EXISTS profiles (
                                        id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                        first_name VARCHAR(100),
                                        last_name VARCHAR(100),
                                        birth_date DATE,
                                        position VARCHAR(50),
                                        level VARCHAR(1),
                                        number INTEGER,
                                        team VARCHAR(100),
                                        email VARCHAR(100),
                                        avatar_url TEXT,
                                        home_city_id BIGINT REFERENCES city(id)
);

-- Таблица ads
CREATE TABLE IF NOT EXISTS ads (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   type INTEGER NOT NULL CHECK (type BETWEEN 1 AND 5),
                                   status VARCHAR(20) NOT NULL CHECK (status IN ('MODERATION', 'ACTIVE', 'FILLED', 'ARCHIVED')),
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   start_time TIMESTAMP NOT NULL,
                                   level TEXT[],
                                   city_id BIGINT NOT NULL REFERENCES city(id),
                                   team VARCHAR(200),
                                   contact_name VARCHAR(100),
                                   contact_phone VARCHAR(20),
                                   rink_ids BIGINT[],
                                   details JSONB
);

-- Таблица responses
CREATE TABLE IF NOT EXISTS responses (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         ad_id UUID NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
                                         user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
                                         message TEXT,
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         UNIQUE(ad_id, user_id)
);

-- Таблица refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                              token VARCHAR(500) NOT NULL UNIQUE,
                                              expiry_date TIMESTAMP NOT NULL,
                                              user_agent VARCHAR(500),
                                              ip_address VARCHAR(45)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_ads_author ON ads(author_id);
CREATE INDEX IF NOT EXISTS idx_ads_city ON ads(city_id);
CREATE INDEX IF NOT EXISTS idx_ads_status ON ads(status);
CREATE INDEX IF NOT EXISTS idx_ads_start_time ON ads(start_time);
CREATE INDEX IF NOT EXISTS idx_responses_ad ON responses(ad_id);
CREATE INDEX IF NOT EXISTS idx_responses_user ON responses(user_id);
CREATE INDEX IF NOT EXISTS idx_rinks_city ON rinks(city_id);