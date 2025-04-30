package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchListRecord;


import java.util.List;

public interface WatchListDao {
    //Добавление фильма в список "Буду смотреть"
    int addToWatchlist(int telegramId, int filmId, String title);
    //Получение списка всех фильмов "Буду смотреть"
    List<WatchListRecord> getWatchlistByTelegramId(int telegramId);
    //Удаление фильма по id из списка "Буду смотреть"
    int deleteFromWatchList(int telegramId, int filmId);
}