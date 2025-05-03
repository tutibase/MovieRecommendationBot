package ru.spbstu.movierecbot.dao.pg;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchListRecord;


import java.util.List;

public interface WatchListDao {
    //Добавление фильма в список "Буду смотреть"
    int addToWatchlist(long telegramId, int filmId, String title);
    //Получение списка всех фильмов "Буду смотреть"
    List<WatchListRecord> getWatchlistByTelegramId(long telegramId);
    //Удаление фильма по id из списка "Буду смотреть"
    int deleteFromWatchList(long telegramId, String filmTitle);
}