package ru.spbstu.movierecbot.dao;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;
import java.util.Optional;

public interface UserDao {

    //Создание нового пользователя
    void createUser(int telegramId);

    //Получение пользователя по Telegram ID
    Optional<UsersRecord> getUserByTelegramId(int telegramId);
}