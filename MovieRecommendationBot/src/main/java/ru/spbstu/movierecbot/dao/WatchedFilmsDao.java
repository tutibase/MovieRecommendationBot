package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchedFilmsRecord;
import java.time.LocalDate;
import java.util.List;

public interface WatchedFilmsDao {
    //Добавление фильма по telegramId пользователя
    int addWatchedFilm(int telegramId, int filmId, String title);

    //Получение фильмов за весь период по telegramId пользователя
    List<WatchedFilmsRecord> getWatchedFilmsByAllPeriod(int telegramId);

    //Получение фильмов за конкретный период по telegramId пользователя(start - начало периода, end - конец)
    List<WatchedFilmsRecord> getWatchedFilmsByExactPeriod(int telegramId, LocalDate start, LocalDate end);

    //Добавление оценки фильму
    void addMarkToFilm(int telegramId, int filmId, int mark);

    //Добавление текстового отзыва фильму
    void addReviewToFilm(int telegramId, int filmId, String review);

}
