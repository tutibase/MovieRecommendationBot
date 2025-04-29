package ru.spbstu.movierecbot.dao.impl;
import ru.spbstu.movierecbot.dao.UserDao;
import ru.spbstu.movierecbot.dbClasses.tables.Users;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private final DSLContext dslContext;

    @Autowired
    public UserDaoImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void createUser(int telegramId) {
        dslContext.insertInto(Users.USERS)
                .set(Users.USERS.TELEGRAM_ID, telegramId)
                .execute();
    }

    @Override
    public Optional<UsersRecord> getUserByTelegramId(int telegramId) {
        return dslContext.selectFrom(Users.USERS)
                .where(Users.USERS.TELEGRAM_ID.eq(telegramId))
                .fetchOptional();
    }
}