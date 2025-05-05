# Movie Recommendation Bot

🎬Movie Recommendation Bot🎬 - Telegram-бот для подбора фильмов на основе предпочтений/случайным образом или по определенным категориям (жанр, год выпуска, актеры, рейтинг и т.д.). Доступна возможность добавления отзывов и оценок фильмам в списке просмотренных, а также добавление фильмов в список “Буду смотреть”.

 
Ссылка на Docker Hub репозиторий c Docker-образом:  
[![Docker](https://img.shields.io/docker/pulls/lizokk/movierecommendationbot)](https://hub.docker.com/r/lizokk/movierecommendationbot)

Ссылка на Telegram-бота:  
[![Telegram Bot](https://img.shields.io/badge/Telegram-Try%20it-blue)](https://t.me/MovieRecSpringBot)

## Содержание
- [Технологический стек](#технологический-стек)
- [Функциональность](#функциональность)
- [Установка и запуск](#установка-и-запуск)
    - [Локальная установка](#локальная-установка)
    - [Docker Hub](#docker-hub)
- [Структура проекта](#структура-проекта)
- [Генерация API документации](#генерация-api-документации)
- [Демонстрация работы бота](#демонстрация-работы-бота)
- [Авторы](#авторы)


## Технологический стек
- **Язык**: Java 23
- **База данных**: PostgreSQL 15
- **Telegram API**: приложение интегрируется с Telegram API для обеспечения функциональности бота
- **Инструменты**:
    - Maven 3.11.0
    - Spring 6.2.5
    - Docker 28.04
    - JUnit 5.10.1
    - JOOQ 3.16.5
    - Spring REST Docs 3.0.0
    - WebFlux 6.2.5
- **Развертывание и контейнеризация:** Docker-desktop

## Функциональность

| Функциональность                | Команды                                                                  |
|---------------------------------|--------------------------------------------------------------------------|
| **Идентификация пользователя**  | `/start`                                                                 |
| **Главное меню**                | `/menu`                                                                  |
| **Информация о фильме**         | `/infoAboutFilm` → ввести название                                       |
| **Управление предпочтениями**   | `/preferences`                                                           |
| → Показать текущие предпочтения | `/preferences` → "Показать" или `/showMyPreferences`                       |
| → Добавить новые предпочтения   | `/preferences` → "Добавить" или `/addPreferences`                        |
| → → Добавить жанры              | `/addPreferences` → "Жанры" или `/addGenrePreferences` [жанр1, жанр2]           |
| → → Добавить актеров            | `/addPreferences` → "Актеры" или `/addActorPreferences` [актер1, актер2]        |
| → → Добавить страны             | `/addPreferences` → "Страны" или `/addCountryPreferences` [страна1, страна2]    |
| → → Добавить годы               | `/addPreferences` → "Годы" или `/addYearPreferences` [2000-2005 или 2000,2001]  |
| → Удалить предпочтения          | `/preferences` → "Удалить" или `/deletePreferences`                      |
| → → Удалить жанры               | `/deleteGenrePreferences` → "Жанры" или `/deleteGenrePreferences` [жанр1]            |
| → → Удалить актеров             | `/deleteActorPreferences` → "Актеры" или `/deleteActorPreferences` [актер1]          |
| → → Удалить страны              | `/deleteCountryPreferences` → "Страны" или `/deleteCountryPreferences` [страна1]       |
| → → Удалить годы                | `/deleteYearPreferences` → "Годы" или `/deleteYearPreferences` [2000-2005]          |
| **Поиск фильмов**               | `/searchfilm`                                                            |
| → Поиск по фильтрам             | `/searchfilm` → "Фильтры" или `/searchByFilters`                         |
| → → Выбрать жанры               | `/chooseGenres` [комедия, фантастика]                                    |
| → → Выбрать актеров             | `/chooseActors` [актер1, актер2]                                         |
| → → Выбрать рейтинг             | `/chooseRate` [7-9, 5, 6]                                                |
| → → Выбрать длительность        | `/chooseDuration` [90-120]                                               |
| → → Выбрать годы                | `/chooseYears` [2010-2020]                                               |
| → → Выбрать страны              | `/chooseCountry` [США, Франция]                                          |
| → → Применить фильтры           | `/applyFilters`                                                          |
| → Посмотреть фильтры            | `/applyFilters`                                                          |
| → Поиск по вашим предпочтениям | `/searchFilm` → "По предпочтениям" или `/searchByPref`                   |
| → Случайный фильм     | `/searchFilm` → "Случайный" или `/searchRandom`                          |
| **Список "Буду смотреть"** | `/watchlist`                                                             |
| → Показать список       | `/watchlist` → "Показать" или `/showWatchList`                           |
| → Добавить фильм        | `/watchlist` → "Добавить" или `/addToWatchList` [название]               |
| → Удалить фильм         | `/watchlist` → "Удалить" или `/deleteFromWatchList` [название]           |
| **Просмотренные фильмы** | `/watchedlist`                                                      |
| → Показать список       | `/watchedFilmsList` → "Показать" или `/showWatchedFilmsList`             |
| → → За период           | `/lastMonth`, `/lastYear`, `/allPeriod` или `/exactPeriod` [дата-дата]   |
| → Добавить фильм        | `/watchedFilmsList` → "Добавить" или `/addToWatchedFilmsList` [название] |
| → Добавить оценку       | `/addMarkToWatchedFilm` [название] → [оценка 0-10]                       |
| → Добавить отзыв        | `/addReviewToWatchedFilm` [название] → [текст отзыва]                    |
| **Администрирование**   | (запуск через командную строку)                                          |
| → Проверка состояния    | `curl http://localhost:8110/healthcheck`                                 |
| → Список пользователей (админ) | `curl http://localhost:8110/admin/users?password=your_admin_password`    |


## Установка и запуск

### Локальная установка
1. Требования
   - Java 21+
   - Maven 3.9+
   - PostgreSQL 15+
2. Клонировать репозиторий:
    ```bash
    git clone https://github.com/tutibase/MovieRecommendationBot.git
    cd MovieRecommendationBot
    ```
3. Инициализировать БД пользователей:
    ```bash
    psql -U postgres -f src/main/resources/users_db.sql
    ```

4. Собрать и запустить:
    ```bash
    mvn clean package
    java -jar target/MovieRecommendationBot-1.0-SNAPSHOT.jar
    ```

### Docker Hub

1. Получить образ с Docker Hub
    ```bash
    docker pull lizokk/movierecommendationbot:latest
    ```

2. Создать файл .env в корне проекта:

    ```declarative
    DB_NAME=users_db
    DB_USERNAME=postgres
    DB_PASSWORD=your_password
    DB_PORT=5432
    DB_URL=jdbc:postgresql://db:5432/users_db
    BOT_TOKEN=your_telegram_bot_token
    BOT_USERNAME=your_bot_username
    HTTP_PORT=8080
    HTTP_HOST=0.0.0.0
    API_KEY=your_key_to_external_api
    ```
3. Создать файл docker-compose.yml:
```
version: '3.8'

services:
  db:
    image: postgres:15
    container_name: movie_recommendation_db
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./users_db.sql:/docker-entrypoint-initdb.d/users_db.sql
    ports:
      - "${DB_PORT}:${DB_PORT}"

  app:
    image: lizokk/movierecommendationbot
    container_name: movie_recommendation_app
    env_file: .env
    environment:
      DB_URL: ${DB_URL}

    depends_on:
      - db
    ports:
      - "${HTTP_PORT}:${HTTP_PORT}"

volumes:
  postgres_data:
```

4. Запустить сервисы в терминале из папки с проектом:
   ```bash
    docker pull lizokk/movierecommendationbot
    docker-compose up -d
   ```
5. Открыть Telegram-бота и начать использование.


## Структура проекта


### 1. Конфигурация (`config`)
- `AppConfig`: основные настройки приложения.
- `DataBaseConfig`: конфигурация подключения к базе данных.
- `JooqConfig`: настройки для JOOQ (библиотека работы с SQL
  и автоматической генерацией классов).
- `WebClientConfig`: конфигурация HTTP-клиента.
- `WebConfig`: общие настройки веб-слоя.

### 2. Контроллеры (`controller`)
- `AdminController`: обработка административных запросов(/users - получение списка
  пользователей).
- `HealthCheckController`: проверка работоспособности сервиса(/healthcheck).
- `MovieRecBot`: основной контроллер телеграм-бота(реализация автомата состояний).

### 3. Data Access Object (`dao`)
- `filmapi`: взаимодействие с внешним API фильмов.
    - `ActorApiDao`: интерфейс для работы с актерами, полученными через внешнее API.
    - `CountryApiDao`: интерфейс для работы со странами, полученными через внешнее API.
    - `FilmDao`: интерфейс для работы с фильмами, полученными через внешнее API.
    - `GenreApiDao`: интерфейс для работы с жанрами, полученными через внешнее API.
- `pg`: взаимодействие с базой данных PostgreSQL.
    - `ActorsDao`: интерфейс для работы с актерами через базу данных.
    - `CountriesDao`: интерфейс для работы со странами через базу данных.
    - `GenreDao`: интерфейс для работы с жанрами через базу данных.
    - `YearDao`: интерфейс для работы с годами через базу данных.
    - `WatchedFilmsDao`: интерфейс для работы со списком "Просмотренные фильмы" через базу данных.
    - `WatchListDao`: интерфейс для работы со списком "Буду смотреть" через базу данных.
    - `UserDao`: интерфейс для работы с пользователями через базу данных.

### 4. Сервисы (`services`)
- `AdminService`: логика административных функций(обработка /users).
- `CategoryCheckService`: валидация ввода пользователя по различным категориям.
- `InfoAboutFilmService`: предоставление информации о фильмах.
- `KeyboardService`: генерация клавиатур для бота.
- `MenuService`: управление главным меню интерфейса.
- `PreferencesService`: работа с предпочтениями пользователя.
- `SearchFilmService`: работа с поиском фильмов по различным параметрам.
- `UserService`: работа с данными пользователей.
- `WatchedListService`: работа со списком "Просмотренные фильмы".
- `WatchListService`: работа со списком "Буду смотреть".
- `state`: управление состоянием бота.
    - `UserState`: перечисление состояний.
    - `StateService` - установка и получение состояний.

### 5. Data Transfer Objects (`dto`)
- `ActorDto`: данные об актерах, полученные через внешнее API.
- `FilmDto`: информация о фильме, полученная через внешнее API.
- `SearchParamsDto`: параметры поиска через внешнее API.
- `UserDto`: данные пользователя, выводимые при административном запросе (/users).

### 6. Прочие компоненты
- `exception`: обработка неавторизованных запросов.


## Генерация API документации

Для автоматической генерации документации API используется Spring Rest Docs.  
Документация создается на основе тестов, расположенных в папке `src/test/java/ru/spbstu/movierecbot/controller`.

#### Как сгенерировать документацию:

1. Выполните команду:
   ```bash
    mvn clean package
   ```
2. После успешного выполнения сборки проекта документация будет автоматически сгенерирована в формате HTML.
3. Сгенерированный файл документации `api-guide.html` будет находиться в папке `target/docs/`.

## Демонстрация работы бота
Ниже представлены GIF-анимации, иллюстрирующие ключевые команды бота.

- **Информация о фильме (команда `/infoaboutfilm`)**

На gif изображении продемонстрирован результат работы бота при вводе
команды `/infoaboutfilm`
![Описание GIF](gifs/info.gif)

- **Поиск фильмов (команда `/searchfilm`)**

На gif изображении продемонстрирован результат работы бота при вводе
команды `/searchfilm` и некоторых последующих команд:
![Описание GIF](gifs/search.gif)

- **Предпочтения (команда `/preferences`)**

На gif изображении продемонстрирован результат работы бота при вводе
команды `/preferences` и некоторых последующих команд:
![Описание GIF](gifs/preferences.gif)

- **Список "Просмотренные фильмы" (команда `/watchedfilm`)**

На gif изображении продемонстрирован результат работы бота при вводе
команды `/watchedfilm` и некоторых последующих команд:
![Описание GIF](gifs/watchedlist.gif)

- **Список "Буду смотреть" (команда `/watchlist`)**

На gif изображении продемонстрирован результат работы бота при вводе
команды `/watchlist` и некоторых последующих команд:
![Описание GIF](gifs/watchlist.gif)


## Авторы

- Якунин Дмитрий
- Луговенко Полина
- Богданова Елизавета
