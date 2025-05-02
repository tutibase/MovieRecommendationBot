package ru.spbstu.movierecbot.dao.pg.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.pg.YearDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.YearsRecord;

import java.util.List;

import static ru.spbstu.movierecbot.dbClasses.Tables.YEARS;

@Repository
public class YearDaoImpl implements YearDao {

    private final DSLContext dslContext;

    @Autowired
    public YearDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int addYear(long telegramId, int year) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(YEARS)
                        .where(YEARS.TELEGRAM_ID.eq(telegramId))
                        .and(YEARS.VALUE.eq(year))
        );

        if (!exists) {
            return dslContext.insertInto(YEARS)
                    .set(YEARS.TELEGRAM_ID, telegramId)
                    .set(YEARS.VALUE, year)
                    .execute();
        }
        return 0;
    }

    @Override
    public List<YearsRecord> getYearsByTelegramId(long telegramId) {
        return dslContext.selectFrom(YEARS)
                .where(YEARS.TELEGRAM_ID.eq(telegramId))
                .orderBy(YEARS.VALUE.asc())
                .fetchInto(YearsRecord.class);
    }

    @Override
    public int deleteYear(long telegramId, int year) {
        return dslContext.deleteFrom(YEARS)
                .where(YEARS.TELEGRAM_ID.eq(telegramId))
                .and(YEARS.VALUE.eq(year))
                .execute();
    }
}
