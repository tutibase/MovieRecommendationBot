package ru.spbstu.movierecbot.services;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
public class MenuService {
    public Mono<String> showMainMenu() {
        return Mono.just(
                """
                🎬 <b>Главное меню</b> 🎬
                
                🎥 <b>Информация о фильме</b>
                /infoaboutfilm - Получить информацию
                
                ⭐ <b>Ваши предпочтения</b>
                /preferences - Настроить рекомендации
                
                🔍 <b>Поиск фильмов</b>
                /searchfilm - Найти фильмы
                
                📋 <b>Ваши списки:</b>
                /watchedlist - Просмотренные фильмы
                /watchlist - Буду смотреть
                
                ℹ️ Для вызова этого меню введите /menu
                """
        );
    }
}
