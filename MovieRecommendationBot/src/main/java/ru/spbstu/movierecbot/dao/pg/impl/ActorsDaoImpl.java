package ru.spbstu.movierecbot.dao.pg.impl;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.spbstu.movierecbot.dao.pg.ActorsDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.ActorsRecord;
import java.util.List;
import static ru.spbstu.movierecbot.dbClasses.tables.Actors.ACTORS;

@Repository
public class ActorsDaoImpl implements ActorsDao {
    private final DSLContext dslContext;

    @Autowired
    public ActorsDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int addActor(long telegramId, int actorId, String fullName){
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(ACTORS)
                        .where(ACTORS.TELEGRAM_ID.eq(telegramId))
                        .and(ACTORS.ACTOR_ID.eq(actorId)));
        if (!exists){
            return dslContext.insertInto(ACTORS)
                    .set(ACTORS.TELEGRAM_ID, telegramId)
                    .set(ACTORS.ACTOR_ID, actorId)
                    .set(ACTORS.FULL_NAME, fullName)
                    .execute();
        }
        return 0;
    }

    @Override
    public List<ActorsRecord> getActorsByTelegramId(long telegramId) {
        return dslContext.selectFrom(ACTORS)
                .where(ACTORS.TELEGRAM_ID.eq(telegramId))
                .fetch();
    }

    @Override
    public int deleteActor(long telegramId, String fullName) {
        return dslContext.deleteFrom(ACTORS)
                .where(ACTORS.TELEGRAM_ID.eq(telegramId))
                .and(ACTORS.FULL_NAME.eq(fullName))
                .execute();

    }

}


