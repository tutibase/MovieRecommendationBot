package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.ActorsRecord;
import ru.spbstu.movierecbot.dbClasses.tables.records.CountriesRecord;
import java.util.List;


public interface CountriesDao {
    //Добавление страны
    int addCountry(int telegramId, String countryName);
    //Получение списка стран
    List<CountriesRecord> getCountriesByTelegramId(int telegramId);
    //Удаление страны
    int deleteCountry(int telegramId, String countryName);
}
