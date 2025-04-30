package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.GenresRecord;
import java.util.List;

public interface GenreDao {
    //Добавление жанра предпочтения пользователя
    int addGenre(long telegramId, String name);

    //Получение всех жанров из предпочтений пользователя
    List<GenresRecord> getGenresByTelegramId(long telegramId);

    //Удаление жанра из предпочтений
    int deleteGenre(long telegramId, String genre);
}
