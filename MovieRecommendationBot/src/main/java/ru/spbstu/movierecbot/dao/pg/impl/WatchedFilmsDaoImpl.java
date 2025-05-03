package ru.spbstu.movierecbot.dao.pg.impl;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.pg.WatchedFilmsDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchedFilmsRecord;
import java.time.LocalDate;
import java.util.List;
import static ru.spbstu.movierecbot.dbClasses.tables.WatchedFilms.WATCHED_FILMS;

@Repository
public class WatchedFilmsDaoImpl implements WatchedFilmsDao {

    private final DSLContext dslContext;

    @Autowired
    public WatchedFilmsDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int addWatchedFilm(long telegramId, int filmId, String title) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(WATCHED_FILMS)
                        .where(WATCHED_FILMS.TELEGRAM_ID.eq(telegramId))
                        .and(WATCHED_FILMS.FILM_ID.eq(filmId))
        );
        if(!exists) {
            return dslContext.insertInto(WATCHED_FILMS)
                    .set(WATCHED_FILMS.TELEGRAM_ID, telegramId)
                    .set(WATCHED_FILMS.FILM_ID, filmId)
                    .set(WATCHED_FILMS.TITLE, title)
                    .set(WATCHED_FILMS.CREATED_AT, LocalDate.now())
                    .execute();
        }
        return 0;
    }

    @Override
    public List<WatchedFilmsRecord> getWatchedFilmsByAllPeriod(long telegramId) {
        return dslContext.selectFrom(WATCHED_FILMS)
                .where(WATCHED_FILMS.TELEGRAM_ID.eq(telegramId))
                .orderBy(WATCHED_FILMS.CREATED_AT.desc())
                .fetchInto(WatchedFilmsRecord.class);
    }

    @Override
    public List<WatchedFilmsRecord> getWatchedFilmsByExactPeriod(long telegramId, LocalDate start, LocalDate end) {
        return dslContext.selectFrom(WATCHED_FILMS)
                .where(WATCHED_FILMS.TELEGRAM_ID.eq(telegramId))
                .and(WATCHED_FILMS.CREATED_AT.between(start, end))
                .orderBy(WATCHED_FILMS.CREATED_AT.desc())
                .fetchInto(WatchedFilmsRecord.class);
    }

    @Override
    public void addMarkToFilm(long telegramId, int filmId, int mark) {
        dslContext.update(WATCHED_FILMS)
                .set(WATCHED_FILMS.RATING, mark)
                .where(WATCHED_FILMS.TELEGRAM_ID.eq(telegramId))
                .and(WATCHED_FILMS.FILM_ID.eq(filmId))
                .execute();
    }

    @Override
    public void addReviewToFilm(long telegramId, int filmId, String review) {
        dslContext.update(WATCHED_FILMS)
                .set(WATCHED_FILMS.REVIEW, review)
                .where(WATCHED_FILMS.TELEGRAM_ID.eq(telegramId))
                .and(WATCHED_FILMS.FILM_ID.eq(filmId))
                .execute();
    }
}