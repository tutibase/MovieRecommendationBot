package ru.spbstu.movierecbot.dao.pg;
import ru.spbstu.movierecbot.dbClasses.tables.records.ActorsRecord;
import java.util.List;

public interface ActorsDao {
    //Добавление актера
    int addActor(long telegramId, int actorId, String fullName);
    //Получение списка актеров
    List<ActorsRecord> getActorsByTelegramId(long telegramId);
    //Удаление актера
    int deleteActor(long telegramId, String fullName);
}