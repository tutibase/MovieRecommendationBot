package ru.spbstu.movierecbot.dao;

import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dbClasses.tables.records.YearsRecord;

import java.util.List;

@Repository
public interface YearDao {

    //Добавление года в предпочтения
    int addYear(int telegramId, int year);

    //Получение всех годов из предпочтений пользователя
    List<YearsRecord> getYearsByTelegramId(int telegramId);

    //Удаление предпочтения по годам
    int deleteYear(int telegramID, int year);

}
