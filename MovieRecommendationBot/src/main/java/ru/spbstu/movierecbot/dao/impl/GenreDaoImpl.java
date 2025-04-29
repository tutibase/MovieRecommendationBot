package ru.spbstu.movierecbot.dao.impl;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.GenreDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.GenresRecord;
import java.util.List;
import static ru.spbstu.movierecbot.dbClasses.tables.Genres.GENRES;

@Repository
public class GenreDaoImpl implements GenreDao {

    private final DSLContext dslContext;

    @Autowired
    public GenreDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int addGenre(int telegramId, String name) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(GENRES)
                        .where(GENRES.TELEGRAM_ID.eq(telegramId))
                        .and(GENRES.NAME.eq(name))
        );

        if (!exists) {
            return dslContext.insertInto(GENRES)
                    .set(GENRES.TELEGRAM_ID, telegramId)
                    .set(GENRES.NAME, name)
                    .execute();
        }
        return 0;
    }

    @Override
    public List<GenresRecord> getGenresByTelegramId(int telegramId) {
        return dslContext.selectFrom(GENRES)
                .where(GENRES.TELEGRAM_ID.eq(telegramId))
                .orderBy(GENRES.NAME.asc())
                .fetchInto(GenresRecord.class);
    }

    @Override
    public int deleteGenre(int telegramId, String genre) {
        return dslContext.deleteFrom(GENRES)
                .where(GENRES.TELEGRAM_ID.eq(telegramId))
                .and(GENRES.NAME.eq(genre))
                .execute();
    }
}