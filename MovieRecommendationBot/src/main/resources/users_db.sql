-- 1. Создаем таблицу Users
CREATE TABLE IF NOT EXISTS users (
    telegram_id BIGINT PRIMARY KEY,
    created_at DATE DEFAULT CURRENT_DATE
);

-- 2. Создаем таблицу WatchedFilms
CREATE TABLE IF NOT EXISTS watched_films (
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    film_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at DATE DEFAULT CURRENT_DATE,
    rating INTEGER CHECK (rating BETWEEN 1 AND 10),
    review TEXT,
    UNIQUE(telegram_id, film_id)
);

-- 3. Создаем таблицу Actors
CREATE TABLE IF NOT EXISTS actors (
    actor_id INTEGER NOT NULL,
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    UNIQUE(telegram_id, actor_id)
);

-- 4. Создаем таблицу Years
CREATE TABLE IF NOT EXISTS years (
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    UNIQUE(telegram_id, value)
);

-- 5. Создаем таблицу countries
CREATE TABLE IF NOT EXISTS countries (
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    UNIQUE(telegram_id, name)
);

-- 6. Создаем таблицу genres
CREATE TABLE IF NOT EXISTS genres (
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    UNIQUE(telegram_id, name)
);

-- 7. Создаем таблицу watch_list
CREATE TABLE IF NOT EXISTS watch_list (
    film_id SERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL REFERENCES users(telegram_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    UNIQUE(telegram_id, film_id)
);

-- 8. Создаем индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_watched_films_telegram_id ON watched_films(telegram_id);
CREATE INDEX IF NOT EXISTS idx_actors_telegram_id ON actors(telegram_id);
CREATE INDEX IF NOT EXISTS idx_years_telegram_id ON years(telegram_id);
CREATE INDEX IF NOT EXISTS idx_countries_telegram_id ON countries(telegram_id);
CREATE INDEX IF NOT EXISTS idx_watch_list_telegram_id ON watch_list(telegram_id);
CREATE INDEX IF NOT EXISTS idx_genres_telegram_id ON genres(telegram_id);
