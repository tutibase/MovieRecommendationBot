package ru.spbstu.movierecbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record FilmDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String russianTitle,
        @JsonProperty("year") int premiereYear,
        @JsonProperty("rating") Rating rating,
        @JsonProperty("ageRating") String ageLimit,
        @JsonProperty("genres") List<Genre> genres,
        @JsonProperty("countries") List<Country> countries,
        @JsonProperty("persons") List<Person> personsData,
        @JsonProperty("movieLength") int duration,
        @JsonProperty("budget") Budget budget,
        @JsonProperty("fees") Fees fees,
        @JsonProperty("similarMovies") List<SimilarMovie> similarFilmsData,
        @JsonProperty("description") String description,
        @JsonProperty("isSeries") Boolean isSeries

) {
    // Кастомный геттер для актеров
    public List<String> actors() {
        if (personsData == null){
            return List.of();
        }
        return personsData.stream()
                .filter(p -> "actor".equals(p.enProfession()))
                .map(Person::name)
                .limit(5)
                .toList();
    }

    // Кастомный геттер для похожих фильмов
    public List<String> similarFilms() {
        if (similarFilmsData == null){
            return List.of();
        }
        return similarFilmsData.stream()
                .map(SimilarMovie::name)
                .limit(3)
                .toList();
    }

    public record Rating(
            @JsonProperty("kp") double kinopoiskRating,
            @JsonProperty("imdb") double imdbRating
    ) {}
    public record Budget(@JsonProperty("value") long value) {}

    public record Fee(@JsonProperty("value") long value) {}

    public record Fees(@JsonProperty("world") Fee worldFee) {}

    public record Genre(@JsonProperty("name") String name) {}
    public record Country(@JsonProperty("name") String name) {}
    public record Person(
            @JsonProperty("name") String name,
            @JsonProperty("enProfession") String enProfession
    ) {}
    public record SimilarMovie(@JsonProperty("name") String name) {}
}