package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchedFilmsRecord;
import java.time.LocalDate;
import java.util.List;

public interface WatchedFilmsDao {
    //Добавление фильма по telegramId пользователя
    int addWatchedFilm(long telegramId, int filmId, String title);

    //Получение фильмов за весь период по telegramId пользователя
    List<WatchedFilmsRecord> getWatchedFilmsByAllPeriod(long telegramId);

    //Получение фильмов за конкретный период по telegramId пользователя(start - начало периода, end - конец)
    List<WatchedFilmsRecord> getWatchedFilmsByExactPeriod(long telegramId, LocalDate start, LocalDate end);

    //Добавление оценки фильму
    void addMarkToFilm(long telegramId, int filmId, int mark);

    //Добавление текстового отзыва фильму
    void addReviewToFilm(long telegramId, int filmId, String review);

}
