package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.ActorsRecord;
import java.util.List;

public interface ActorsDao {
    //Добавление актера
    int addActor(int telegramId, String fullName);
    //Получение списка актеров
    List<ActorsRecord> getActorsByTelegramId(int telegramId);
    //Удаление актера
    int deleteActor(int telegramId, String fullName);
}