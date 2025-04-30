package ru.spbstu.movierecbot.dao.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.CountriesDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.CountriesRecord;
import java.util.List;
import static ru.spbstu.movierecbot.dbClasses.tables.Countries.COUNTRIES;

@Repository
public class CountriesDaoImpl implements CountriesDao {

    private final DSLContext dslContext;

    @Autowired
    public CountriesDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int addCountry(int telegramId, String countryName) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(COUNTRIES)
                        .where(COUNTRIES.TELEGRAM_ID.eq(telegramId))
                        .and(COUNTRIES.NAME.eq(countryName)));

        if (!exists){
            return dslContext.insertInto(COUNTRIES)
                    .set(COUNTRIES.TELEGRAM_ID, telegramId)
                    .set(COUNTRIES.NAME, countryName)
                    .execute();
        }
        return 0;
    }

    @Override
    public List<CountriesRecord> getCountriesByTelegramId(int telegramId) {
        return dslContext.selectFrom(COUNTRIES)
                .where(COUNTRIES.TELEGRAM_ID.eq(telegramId))
                .fetch();
    }

    @Override
    public int deleteCountry(int telegramId, String countryName) {
        return dslContext.deleteFrom(COUNTRIES)
                .where(COUNTRIES.TELEGRAM_ID.eq(telegramId))
                .and(COUNTRIES.NAME.eq(countryName))
                .execute();

    }
}


