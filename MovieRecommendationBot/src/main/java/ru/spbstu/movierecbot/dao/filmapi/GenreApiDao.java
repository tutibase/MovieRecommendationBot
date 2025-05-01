package ru.spbstu.movierecbot.dao.filmapi;

import reactor.core.publisher.Flux;

public interface GenreApiDao {
    // Получить список всех жанров
    Flux<String> getAllGenres();
}
