package ru.spbstu.movierecbot.dao.filmapi.impl;

import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dto.ApiResponseDto;
import ru.spbstu.movierecbot.dto.FilmDto;

import java.util.List;


@Repository
public class FilmDaoImpl implements FilmDao {

    private final WebClient kinopoiskWebClient;

    public FilmDaoImpl(WebClient kinopoiskWebClient) {
        this.kinopoiskWebClient = kinopoiskWebClient;
    }

    @Override
    public Mono<FilmDto> getFilmById(Integer filmId) {
        List<String> selectFields = List.of(
                "id", "name", "description",
                "year", "rating", "ageRating", "budget", "movieLength", "genres", "countries",
                "persons", "fees", "premiere", "similarMovies");

        return kinopoiskWebClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/movie")
                            .queryParam("page", 1)
                            .queryParam("limit", 1)
                            .queryParam("id", filmId);

                    // Добавляем каждый selectField как отдельный параметр
                    for (String field : selectFields) {
                        builder.queryParam("selectFields", field);
                    }

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(ApiResponseDto.class)
                .flatMap(response -> {
                    if (response.films().isEmpty()) {
                        return Mono.error(new RuntimeException("Фильм не найден"));
                    }
                    return Mono.just(response.films().getFirst());
                });
    }

    @Override
    public Mono<FilmDto> getFilmByName(String filmName) {
        return getFilmIdByName(filmName)
                .flatMap(this::getFilmById);
    }

    @Override
    public Mono<Integer> getFilmIdByName(String filmName) {
        return kinopoiskWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/search")
                        .queryParam("page", 1)
                        .queryParam("limit", 1)
                        .queryParam("query", filmName)
                        .build())
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(ApiResponseDto.class) // Десериализуем ответ в ApiResponseDto
                .flatMap(apiResponse -> {
                    if (apiResponse.films() == null || apiResponse.films().isEmpty()) {
                        return Mono.error(new RuntimeException("Фильм не найден"));
                    }
                    return Mono.just(apiResponse.films().getFirst().id());
                });
    }
}
