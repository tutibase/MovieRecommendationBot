package ru.spbstu.movierecbot.dao.pg.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.pg.WatchListDao;
import java.util.List;

import ru.spbstu.movierecbot.dbClasses.tables.records.WatchListRecord;

import static ru.spbstu.movierecbot.dbClasses.tables.WatchList.WATCH_LIST;


@Repository
public class WatchListDaoImpl implements WatchListDao {

    private final DSLContext dslContext;

    @Autowired
    public WatchListDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }


    @Override
    public int addToWatchlist(long telegramId, int filmId, String title) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(WATCH_LIST)
                .where(WATCH_LIST.TELEGRAM_ID.eq(telegramId))
                .and(WATCH_LIST.FILM_ID.eq(filmId)));
        if (!exists){
            return dslContext.insertInto(WATCH_LIST)
                    .set(WATCH_LIST.TELEGRAM_ID, telegramId)
                    .set(WATCH_LIST.FILM_ID, filmId)
                    .set(WATCH_LIST.TITLE, title)
                    .execute();
        }
        return 0;
    }


    @Override
    public List<WatchListRecord> getWatchlistByTelegramId(long telegramId) {
        return dslContext.selectFrom(WATCH_LIST)
                .where(WATCH_LIST.TELEGRAM_ID.eq(telegramId))
                .fetch();
    }

    @Override
    public int deleteFromWatchList(long telegramId, int filmId) {
        return dslContext.deleteFrom(WATCH_LIST)
                .where(WATCH_LIST.TELEGRAM_ID.eq(telegramId))
                .and(WATCH_LIST.FILM_ID.eq(filmId))
                .execute();

    }
}