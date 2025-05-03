package ru.spbstu.movierecbot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dao.pg.WatchListDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchListRecord;

import java.util.List;
import java.util.Stack;

@Service
public class WatchListService {
    private final WatchListDao watchListDao;
    private final FilmDao filmDao;

    @Autowired
    public WatchListService(WatchListDao watchListDao, FilmDao filmDao) {
        this.watchListDao = watchListDao;
        this.filmDao = filmDao;
    }

    //обработчик на команду /showWatchList
    public Mono<String> showWatchList(long telegramId) {
        return Mono.fromCallable(() -> {
            List<WatchListRecord> watchFilmList = watchListDao.getWatchlistByTelegramId(telegramId);

            if (watchFilmList.isEmpty()) {
                return "📭 <b>Ваш список \"Буду смотреть\" пуст</b>\n" +
                        "Добавьте первый фильм с помощью /addToWatchList";
            }

            StringBuilder result = new StringBuilder();
            result.append("📋 <b>Ваш список \"Буду смотреть\":</b>\n\n");
            watchFilmList.forEach(film -> {
                result.append("• ").append(film.getTitle());
                result.append("\n");
            });
            return result.toString();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    //Обработчик команды /addToWatchList
    public Mono<String> addToWatchList(long telegramId, String filmTitle) {
        return filmDao.getFilmByName(filmTitle)
                .flatMap(filmDto -> Mono.fromCallable(() -> {
                            if (watchListDao.addToWatchlist(telegramId, filmDto.id(), filmDto.russianTitle()) != 0) {
                                return "✅ Фильм \"" + filmDto.russianTitle() +
                                        "\" добавлен в \"Буду смотреть\"! 🎬";
                            } else {
                                return "ℹ️ Фильм \"" + filmDto.russianTitle() +
                                        "\" уже есть в вашем списке \"Буду смотреть\"";
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())) // Выносим блокирующую операцию
                .onErrorResume(error -> Mono.just("⚠️ Фильм \"" + filmTitle +
                        "\" не найден\nСписок \"Буду смотреть\" не изменён"));
    }

    //Обработчик команды /deleteFromWatchList
    public Mono<String> deleteFromWatchList(long telegramId, String filmTitle) {
        return Mono.fromCallable(() -> watchListDao.deleteFromWatchList(telegramId, filmTitle))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    if (result != 0) {
                        return Mono.just("🗑️ Фильм \"" + filmTitle +
                                "\" удалён из \"Буду смотреть\"");
                    } else {
                        return Mono.just("🔍 Фильма \"" + filmTitle +
                                "\" не было в вашем списке \"Буду смотреть\"");
                    }
                })
                .onErrorResume(e -> Mono.just("⚠️ Ошибка при удалении фильма"));
    }
}

