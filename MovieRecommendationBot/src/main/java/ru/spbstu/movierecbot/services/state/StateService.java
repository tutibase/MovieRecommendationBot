package ru.spbstu.movierecbot.services.state;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateService {
    // Карта для хранения состояний пользователей (ключ: chatId)
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    // Установить состояние
    public void setState(Long chatId, UserState state) {
        userStates.put(chatId, state);
    }

    // Получить состояние
    public UserState getState(Long chatId) {
        return userStates.get(chatId);
    }

    // Очистить состояние
    public void clearState(Long chatId) {
        userStates.remove(chatId);
    }
}