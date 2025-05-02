package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dto.FilmDto;
import ru.spbstu.movierecbot.dto.SearchParamsDto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


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

                    // Добавляем selectFields
                    selectFields.forEach(field -> builder.queryParam("selectFields", field));

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(ApiResponseDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 3 попытки с интервалом 1 сек
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException("Все попытки запроса завершились неудачей")))
                .flatMap(response -> {
                    if (response.films().isEmpty()) {
                        return Mono.error(new RuntimeException("Фильм не найден"));
                    }
                    return Mono.just(response.films().getFirst());
                });
    }

    @Override
    public Mono<FilmDto> getRandomFilm() {

        return kinopoiskWebClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/movie/random")
                            .queryParam("notNullFields", "id")
                            .queryParam("notNullFields", "name")
                            .queryParam("notNullFields", "description")
                            .queryParam("notNullFields", "rating.kp")
                            .queryParam("rating.kp", "5-10")
                            .queryParam("genres.name", "!документальный")
                            .queryParam("type", "movie");

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(FilmDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 3 попытки с интервалом 1 сек
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException("Все попытки запроса завершились неудачей" + spec + signal)))
                .flatMap(response -> {
                    if (response.id() == null) {
                        return Mono.error(new RuntimeException("Фильм не найден"));
                    }
                    return Mono.just(response);
                });
    }

    @Override
    public Mono<FilmDto> getFilmByName(String filmName) {
        return getFilmIdByName(filmName)
                .flatMap(this::getFilmById);
    }

    @Override
    public Flux<FilmDto> getFilmsByParams(SearchParamsDto params) {
        List<String> selectFields = List.of(
                "id", "name", "description",
                "year", "rating", "ageRating", "budget", "movieLength", "genres", "countries",
                "persons", "fees", "premiere", "similarMovies");

        return kinopoiskWebClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/movie")
                            .queryParam("page", 1)
                            .queryParam("limit", 5);

                    // Добавляем selectFields
                    selectFields.forEach(field -> builder.queryParam("selectFields", field));

                    builder.queryParam("type", "movie");


                    // Добавляем параметры фильтрации только если они не null
                    Optional.ofNullable(params.years()).ifPresent(years ->
                            years.forEach(year -> builder.queryParam("year", year))
                    );
                    Optional.ofNullable(params.ratings()).ifPresent(ratings ->
                            ratings.forEach(rating -> builder.queryParam("rating.kp", rating))
                    );
                    Optional.ofNullable(params.movieLength()).ifPresent(length -> builder.queryParam("movieLength", length)
                    );
                    Optional.ofNullable(params.genres()).ifPresent(genres ->
                            genres.forEach(genre -> builder.queryParam("genres.name", genre))
                    );
                    Optional.ofNullable(params.countries()).ifPresent(countries ->
                            countries.forEach(country -> builder.queryParam("countries.name", country))
                    );
                    Optional.ofNullable(params.actors()).ifPresent(persons ->
                            persons.forEach(actor -> builder.queryParam("persons.id", "+" + actor.id()))
                    );
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(ApiResponseDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 3 попытки с интервалом 1 сек
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException("Все попытки запроса завершились неудачей")))
                .flatMapMany(response -> Flux.fromIterable(response.films()));
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
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 3 попытки с интервалом 1 сек
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException("Все попытки запроса завершились неудачей")))
                .flatMap(apiResponse -> {
                    if (apiResponse.films() == null || apiResponse.films().isEmpty()) {
                        return Mono.error(new RuntimeException("Фильм не найден"));
                    }
                    return Mono.just(apiResponse.films().getFirst().id());
                });
    }

    public record ApiResponseDto(@JsonProperty("docs") List<FilmDto> films) {}
}
