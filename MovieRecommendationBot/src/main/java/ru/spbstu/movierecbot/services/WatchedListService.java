package ru.spbstu.movierecbot.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dao.pg.WatchedFilmsDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.WatchedFilmsRecord;
import ru.spbstu.movierecbot.dto.FilmDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchedListService {
    private static final String DATE_RANGE_REGEX = "\\d{2}\\.\\d{2}\\.\\d{4}\\s*-\\s*\\d{2}\\.\\d{2}\\.\\d{4}";
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final WatchedFilmsDao watchedFilmsDao;
    private final FilmDao filmDao;
    public ConcurrentHashMap<Long, FilmDto> userMarkOrReviewToFilm = new ConcurrentHashMap<>();

    @Autowired
    public WatchedListService(WatchedFilmsDao watchedFilmsDao, FilmDao filmDao) {
        this.watchedFilmsDao = watchedFilmsDao;
        this.filmDao = filmDao;
    }

    public record DateRange(LocalDate startOfPeriod, LocalDate endOfPeriod) {}

    // Обработчик на команду /threeMonths, /lastMonth, /lastYear
    public Mono<String> showWatchedFilmsListByPeriod(long telegramId, String command) {
        return Mono.fromCallable(() -> {
                    LocalDate now = LocalDate.now();
                    DateRange dateRange = new DateRange(now, now);
                    String header = switch (command) {
                        case "/threeMonths" -> {
                            dateRange = new DateRange(now.minusMonths(3), now);
                            yield "Ваши просмотренные фильмы за последние три месяца:\n\n";
                        }
                        case "/lastMonth" -> {
                            dateRange = new DateRange(now.minusMonths(1), now);
                            yield "Ваши просмотренные фильмы за последний месяц:\n\n";
                        }
                        case "/lastYear" -> {
                            dateRange = new DateRange(now.minusYears(1), now);
                            yield "Ваши просмотренные фильмы за последний год:\n\n";
                        }
                        default -> throw new IllegalArgumentException("Неизвестная команда: " + command);
                    };
                    return buildFilmListResponse(telegramId, dateRange.startOfPeriod, dateRange.endOfPeriod, header);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(mono -> mono); // Распаковываем Mono<Mono<String>> → Mono<String>
    }

    // Обработчик на команду /exactPeriod
    public Mono<String> showWatchedFilmsListByExactPeriod(long telegramId, String period) {
        return Mono.fromCallable(() -> {
                    DateRange dateRange = parsePeriod(period);

                    if (dateRange.startOfPeriod.equals(LocalDate.MIN) && dateRange.endOfPeriod.equals(LocalDate.MAX)) {
                        return Mono.<String>just("Период введен некорректно. Вывод фильмов за этот период невозможен.");
                    }

                    String header = "Ваши просмотренные фильмы за период " + period + "\n\n";
                    return buildFilmListResponse(telegramId, dateRange.startOfPeriod, dateRange.endOfPeriod, header);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(mono -> mono); // Распаковываем Mono<Mono<String>> в Mono<String>
    }
    // Обработчик на команду /allPeriod
    public Mono<String> showWatchedFilmsListByAllPeriod(long telegramId) {
        return Mono.fromCallable(() -> {
            StringBuilder result = new StringBuilder("Ваши просмотренные фильмы за все время:\n\n");
            List<WatchedFilmsRecord> filmList = watchedFilmsDao.getWatchedFilmsByAllPeriod(telegramId);

            if (filmList.isEmpty()) {
                return result.append("У вас нет просмотренных фильмов.").toString();
            }

            filmList.forEach(film -> {
                result.append("• ").append(film.getTitle());
                if (film.getRating() != null) {
                    result.append(" \n| Оценка: ").append(film.getRating());
                }
                if (film.getReview() != null && !film.getReview().isEmpty()) {
                    result.append(" \n| Отзыв: ").append(film.getReview());
                }
                result.append("\n");
            });
            return result.toString();
        }).subscribeOn(Schedulers.boundedElastic());
    }


    // Обработчик на команду /addToWatchedFilmsList
    public Mono<String> addToWatchedFilmsList(long telegramId, String filmTitle) {
        return filmDao.getFilmByName(filmTitle)
                .flatMap(filmDto -> Mono.fromCallable(() -> {
                    if(watchedFilmsDao.addWatchedFilm(telegramId, filmDto.id(), filmDto.russianTitle()) !=0){;
                            return "Фильм \"" + filmDto.russianTitle() +
                                    "\" добавлен в список просмотренных фильмов.";}
                    else {
                            return "Фильм \"" + filmDto.russianTitle() +
                                "\" уже есть в вашем списке просмотренных.";}
                })
                        .subscribeOn(Schedulers.boundedElastic())) // Выносим блокирующую операцию
                .onErrorResume(error -> Mono.just("Не удалось найти фильм с названием \"" +
                        filmTitle + "\". В список просмотренных фильмов ничего не добавлено."));
    }

    // Здесь проверяем наличие фильма в БД по API
    public Mono<Integer> checkingForMovieExistence(long telegramId, String filmTitle){
        return filmDao.getFilmByName(filmTitle)
                .flatMap(filmDto -> Mono.fromCallable(() -> {
                            userMarkOrReviewToFilm.put(telegramId, filmDto);
                            //Внутри addWatchedFilm есть обработка дубликатов
                            return watchedFilmsDao.addWatchedFilm(telegramId, filmDto.id(), filmDto.russianTitle());
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(error -> Mono.empty());
    }

    //Проверка введенной оценки
    public Boolean checkFilmMark(String mark){
        String regex = "^(10|[0-9])$";
        return  mark.matches(regex);
    }
    // Обработчик на команду /addScoreToWatchedFilm, который вызывается после checkFilmTitle
    public Mono<String> addMarkToWatchedFilm(long telegramId, String mark) {
        if (checkFilmMark(mark)) {
            int intMark = Integer.parseInt(mark);
            FilmDto filmDto = userMarkOrReviewToFilm.get(telegramId);
            watchedFilmsDao.addMarkToFilm(telegramId, filmDto.id(), intMark);
            userMarkOrReviewToFilm.remove(telegramId);
            return Mono.just("\"Вы поставили фильму \"" + filmDto.russianTitle() +"\" оценку " + intMark);}
        else return Mono.empty();
    }

    // Обработчик на команду /addReviewToWatchedFilm, который вызывается после checkFilmTitle
    public Mono<String> addReviewToWatchedFilm(long telegramId, String review) {
        FilmDto filmDto = userMarkOrReviewToFilm.get(telegramId);
        watchedFilmsDao.addReviewToFilm(telegramId, filmDto.id(), review);
        userMarkOrReviewToFilm.remove(telegramId);
        return Mono.just("\"Вы оставили отзыв к фильму \"" + filmDto.russianTitle() + "\".");
    }

    // Парсер введенного периода
    private DateRange parsePeriod(String periodString) {
        if (periodString == null || !periodString.matches(DATE_RANGE_REGEX)) {
            return new DateRange(LocalDate.MIN, LocalDate.MAX);
        }
        try {
            String[] dates = periodString.split("\\s*-\\s*");
            LocalDate startDate = LocalDate.parse(dates[0], DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(dates[1], DATE_FORMATTER);
            return startDate.isAfter(endDate)
                    ? new DateRange(endDate, startDate)
                    : new DateRange(startDate, endDate);
        } catch (DateTimeParseException e) {
            return new DateRange(LocalDate.MIN, LocalDate.MAX);
        }
    }

    // Сбор ответа
    private Mono<String> buildFilmListResponse(long telegramId, LocalDate startDate, LocalDate endDate, String header) {
        return Mono.fromCallable(() -> {
            StringBuilder result = new StringBuilder(header);
            List<WatchedFilmsRecord> filmList = watchedFilmsDao
                    .getWatchedFilmsByExactPeriod(telegramId, startDate, endDate);

            if (filmList.isEmpty()) {
                return result.append("У вас нет просмотренных фильмов за этот период.").toString();
            }

            filmList.forEach(film -> {
                result.append("• ").append(film.getTitle());
                if (film.getRating() != null) {
                    result.append(" \n| Оценка: ").append(film.getRating());
                }
                if (film.getReview() != null && !film.getReview().isEmpty()) {
                    result.append(" \n| Отзыв: ").append(film.getReview());
                }
                result.append("\n");
            });
            return result.toString();
        }).subscribeOn(Schedulers.boundedElastic());
    }

}