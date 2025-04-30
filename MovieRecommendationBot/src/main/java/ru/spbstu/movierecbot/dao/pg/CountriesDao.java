package ru.spbstu.movierecbot.dao.pg;
import ru.spbstu.movierecbot.dbClasses.tables.records.CountriesRecord;
import java.util.List;


public interface CountriesDao {
    //Добавление страны
    int addCountry(long telegramId, String countryName);
    //Получение списка стран
    List<CountriesRecord> getCountriesByTelegramId(long telegramId);
    //Удаление страны
    int deleteCountry(long telegramId, String countryName);
}
