package ru.spbstu.movierecbot.services;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {
    public Mono<ReplyKeyboardMarkup> menuKeyboardMarkup(){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();


        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎥 Получить информацию"));
        row1.add(new KeyboardButton("⭐ Настроить рекомендации"));


        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🔍 Найти фильмы"));
        row2.add(new KeyboardButton("📋 Просмотренные фильмы"));


        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📋 Буду смотреть"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> searchKeyboardMarkup() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎛️ Поиск по фильтрам"));
        row1.add(new KeyboardButton("❤️ По предпочтениям"));

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🎲 Случайный фильм"));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return Mono.just(keyboard);
    }

    public Mono<ReplyKeyboardMarkup> watchListKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👀 Показать список"));
        row1.add(new KeyboardButton("🎥 Добавить фильм"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("❌ Удалить фильмы"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> preferencesKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👁️ Показать предпочтения"));
        row1.add(new KeyboardButton("➕ Добавить предпочтения"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🗑️ Удалить предпочтения"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> watchedListKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📋 Показать список"));
        row1.add(new KeyboardButton("➕ Добавить фильм"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⭐ Оценить фильм"));
        row2.add(new KeyboardButton("✏️ Написать отзыв"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> addPreferencesKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎭 Жанры"));
        row1.add(new KeyboardButton("👨‍🎤 Актеры"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🌍 Страны"));
        row2.add(new KeyboardButton("📅 Годы"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> deletePreferencesKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎭 Удалить жанры"));
        row1.add(new KeyboardButton("👨‍🎤 Удалить актеров"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🌍 Удалить страны"));
        row2.add(new KeyboardButton("📅 Удалить годы"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }

    public Mono<ReplyKeyboardMarkup> showWatchedPeriodKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("⏳ 3 месяца"));
        row1.add(new KeyboardButton("🗓️ Прошлый месяц"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🎉 Прошлый год"));
        row2.add(new KeyboardButton("🏆 Все время"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔍 Конкретные даты"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return Mono.just(keyboardMarkup);
    }


    public Mono<ReplyKeyboardMarkup> getFiltersKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎉 Жанры"));
        row1.add(new KeyboardButton("🌟 Актеры"));

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⭐ Рейтинг"));
        row2.add(new KeyboardButton("⏱️ Длительность"));

        // Третий ряд
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🗓️ Годы"));
        row3.add(new KeyboardButton("🌍 Страна"));

        // Четвертый ряд
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("✅ Применить"));
        row4.add(new KeyboardButton("\uD83D\uDC40 Посмотреть"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        return Mono.just(keyboard);
    }


    public Mono<String> getCommandFromKeyboard(String text) {
        return switch (text) {
            // Главное меню
            case "🎥 Получить информацию" -> Mono.just("/infoaboutfilm");
            case "⭐ Настроить рекомендации" -> Mono.just("/preferences");
            case "🔍 Найти фильмы" -> Mono.just("/searchfilm");
            case "📋 Просмотренные фильмы" -> Mono.just("/watchedlist");
            case "📋 Буду смотреть" -> Mono.just("/watchlist");

            // Поиск фильмов
            case "🎲 Случайный фильм" -> Mono.just("/searchRandom");
            case "🎛️ Поиск по фильтрам" -> Mono.just("/searchByFilters");
            case "❤️ По предпочтениям" -> Mono.just("/searchByPref");

            // Список "Буду смотреть"
            case "👀 Показать список" -> Mono.just("/showWatchList");
            case "🎥 Добавить фильм" -> Mono.just("/addToWatchList");
            case "❌ Удалить фильмы" -> Mono.just("/deleteFromWatchList");

            // Предпочтения
            case "👁️ Показать предпочтения" -> Mono.just("/showMyPreferences");
            case "➕ Добавить предпочтения" -> Mono.just("/addPreferences");
            case "🗑️ Удалить предпочтения" -> Mono.just("/deletePreferences");

            // Просмотренные фильмы
            case "📋 Показать список" -> Mono.just("/showWatchedFilmsList");
            case "➕ Добавить фильм" -> Mono.just("/addToWatchedFilmsList");
            case "⭐ Оценить фильм" -> Mono.just("/addMarkToWatchedFilm");
            case "✏️ Написать отзыв" -> Mono.just("/addReviewToWatchedFilm");

            // Добавление предпочтений
            case "🎭 Жанры" -> Mono.just("/addGenrePreferences");
            case "👨‍🎤 Актеры" -> Mono.just("/addActorPreferences");
            case "🌍 Страны" -> Mono.just("/addCountryPreferences");
            case "🗓️ Годы" -> Mono.just("/addYearPreferences");

            // Периоды просмотра
            case "⏳ 3 месяца" -> Mono.just("/threeMonths");
            case "🗓️ Прошлый месяц" -> Mono.just("/lastMonth");
            case "🎉 Прошлый год" -> Mono.just("/lastYear");
            case "🏆 Все время" -> Mono.just("/allPeriod");
            case "🔍 Конкретные даты" -> Mono.just("/exactPeriod");


            // Фильтры поиска
            case "🎉 Жанры" -> Mono.just("/chooseGenres");
            case "🌟 Актеры" -> Mono.just("/chooseActors");
            case "⭐ Рейтинг" -> Mono.just("/chooseRate");
            case "⏱️ Длительность" -> Mono.just("/chooseDuration");
            case "📅 Годы" -> Mono.just("/chooseYears");
            case "🌍 Страна" -> Mono.just("/chooseCountry");
            case "\uD83D\uDC40 Посмотреть" -> Mono.just("/showFilters");
            case "✅ Применить" -> Mono.just("/applyFilters");

            default -> Mono.just("Неизвестный текст с клавиатуры");
        };
    }
}
