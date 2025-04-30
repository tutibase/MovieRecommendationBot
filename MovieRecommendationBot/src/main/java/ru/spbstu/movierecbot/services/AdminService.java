package ru.spbstu.movierecbot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import ru.spbstu.movierecbot.dao.UserDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;

@Service
public class AdminService {
    private final UserDao userDao;

    @Value("${admin.password}")
    private String adminPassword;

    public AdminService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Flux<UsersRecord> getUsersIfAdmin(String password) {
        // Проверка пароля и получение пользователей
        return Flux.fromIterable(userDao.getAllUsers())
                .subscribeOn(Schedulers.boundedElastic()); // Для блокирующего DAO
    }
}