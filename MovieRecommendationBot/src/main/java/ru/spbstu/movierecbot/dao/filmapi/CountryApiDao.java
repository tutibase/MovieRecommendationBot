package ru.spbstu.movierecbot.dao.filmapi;

import reactor.core.publisher.Flux;

public interface CountryApiDao {
    // Получить список всех жанров
    Flux<String> getAllCountries();
}
