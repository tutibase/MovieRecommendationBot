package ru.spbstu.movierecbot.services;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
public class MenuService {
    public Mono<String> showMainMenu() {
        return Mono.just(
                """
                🎬 *Главное меню* 🎬
                
                🎥 *Информация о фильме*
                /infoAboutFilm - Получить информацию
                
                ⭐ *Ваши предпочтения*
                /preferences - Настроить рекомендации
                
                🔍 *Поиск фильмов*
                /searchFilm - Найти фильмы
                
                📋 *Ваши списки:*
                /watchedFilmsList - Просмотренные фильмы
                /watchlist - Буду смотреть
                
                ℹ️ Для вызова этого меню введите /menu
                """
        );
    }
}
