package ru.spbstu.movierecbot.dao.filmapi;

import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dto.FilmDto;

public interface FilmDao {
    // Получить инфо о фильме по айди
    Mono<FilmDto> getFilmById(Integer filmId);

    // Получить инфо о фильме по названию
    Mono<FilmDto> getFilmByName(String filmName);

    // Получить айди фильма по его названию
    Mono<Integer> getFilmIdByName(String filmName);
}
