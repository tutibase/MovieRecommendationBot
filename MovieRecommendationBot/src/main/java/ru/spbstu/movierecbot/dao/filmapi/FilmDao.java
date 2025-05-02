package ru.spbstu.movierecbot.dao.filmapi;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dto.FilmDto;
import ru.spbstu.movierecbot.dto.SearchParamsDto;

public interface FilmDao {
    // Получить инфо о фильме по айди
    Mono<FilmDto> getFilmById(Integer filmId);

    // Получить инфо о случайном фильме
    Mono<FilmDto> getRandomFilm();

    // Получить инфо о фильме по названию
    Mono<FilmDto> getFilmByName(String filmName);

    // Получить инфо о фильмах по фильтрам
    public Flux<FilmDto> getFilmsByParams(SearchParamsDto params);

    // Получить айди фильма по его названию
    Mono<Integer> getFilmIdByName(String filmName);
}
