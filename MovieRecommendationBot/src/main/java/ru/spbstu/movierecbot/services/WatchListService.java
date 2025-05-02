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
                return "Ваш список \"Буду смотреть\" пуст.";
            }

            StringBuilder result = new StringBuilder();
            result.append("Ваш список \"Буду смотреть\":\n\n");
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
                                return "Фильм \"" + filmDto.russianTitle() +
                                        "\" добавлен в список \"Буду смотреть\".";
                            } else {
                                return "Фильм \"" + filmDto.russianTitle() +
                                        "\" уже есть в вашем списке \"Буду смотреть\".";
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())) // Выносим блокирующую операцию
                .onErrorResume(error -> Mono.just("Не удалось найти фильм с названием \"" +
                        filmTitle + "\". В список \"Буду смотреть\" ничего не добавлено."));
    }

    //Обработчик команды /deleteFromWatchList
    public Mono<String> deleteFromWatchList(long telegramId, String filmTitle) {
        StringBuilder result = new StringBuilder();
        return filmDao.getFilmByName(filmTitle)
                .flatMap(filmDto -> Mono.fromCallable(() -> {
                            if (watchListDao.deleteFromWatchList(telegramId, filmDto.id()) != 0) {
                                return "Фильм \"" + filmDto.russianTitle() +
                                        "\" удален из списка \"Буду смотреть\".";
                            } else {
                                return "Фильма \"" + filmDto.russianTitle() +
                                        "\" не было в вашем списке \"Буду смотреть\".";
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())) // Выносим блокирующую операцию
                .onErrorResume(error -> Mono.just("Не удалось найти фильм с названием \"" +
                        filmTitle + "\". Из списка \"Буду смотреть\" ничего не удалено."));
    }
}

