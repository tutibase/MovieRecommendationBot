package ru.spbstu.movierecbot.dao.pg.impl;
import ru.spbstu.movierecbot.dao.pg.UserDao;
import ru.spbstu.movierecbot.dbClasses.tables.Users;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import static ru.spbstu.movierecbot.dbClasses.tables.Users.USERS;

@Repository
public class UserDaoImpl implements UserDao {

    private final DSLContext dslContext;

    @Autowired
    public UserDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int createUser(long telegramId) {
        boolean exists = dslContext.fetchExists(
                dslContext.selectFrom(USERS)
                        .where(USERS.TELEGRAM_ID.eq(telegramId))
        );
        if (!exists){
            return dslContext.insertInto(USERS)
                    .set(USERS.TELEGRAM_ID, telegramId)
                    .execute();
        }
        return 0;

    }

    @Override
    public Optional<UsersRecord> getUserByTelegramId(long telegramId) {
        return dslContext.selectFrom(Users.USERS)
                .where(Users.USERS.TELEGRAM_ID.eq(telegramId))
                .fetchOptional();
    }

    @Override
    public List<UsersRecord> getAllUsers() {
        return dslContext.selectFrom(USERS).fetch();
    }
}