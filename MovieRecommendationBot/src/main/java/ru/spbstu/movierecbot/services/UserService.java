package ru.spbstu.movierecbot.services;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;
import ru.spbstu.movierecbot.dao.pg.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void registerUser(long telegramId) {
        userDao.createUser(telegramId);
    }

    public Optional<UsersRecord> findUserByTelegramId(long telegramId) {
        return userDao.getUserByTelegramId(telegramId);
    }

    public List<UsersRecord> getAllUsers(){
        return userDao.getAllUsers();
    }
}