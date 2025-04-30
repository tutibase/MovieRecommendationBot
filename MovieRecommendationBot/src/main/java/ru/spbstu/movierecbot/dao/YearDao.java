package ru.spbstu.movierecbot.dao;

import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dbClasses.tables.records.YearsRecord;

import java.util.List;

@Repository
public interface YearDao {

    //Добавление года в предпочтения
    int addYear(long telegramId, int year);

    //Получение всех годов из предпочтений пользователя
    List<YearsRecord> getYearsByTelegramId(long telegramId);

    //Удаление предпочтения по годам
    int deleteYear(long telegramID, int year);

}
