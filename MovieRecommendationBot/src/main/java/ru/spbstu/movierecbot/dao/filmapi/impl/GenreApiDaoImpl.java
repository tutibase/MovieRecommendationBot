package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dao.filmapi.GenreApiDao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Repository
public class GenreApiDaoImpl implements GenreApiDao {
    private final List<String> genreNames;

    public GenreApiDaoImpl(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("genres.json");
        String json;

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] fileData = FileCopyUtils.copyToByteArray(inputStream);
            json = new String(fileData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка открытия json с жанрами", e);
        }

        try {
            Genre[] genres = objectMapper.readValue(json, Genre[].class);
            this.genreNames = Arrays.stream(genres)
                    .map(Genre::name)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга жанров: ", e);
        }
    }


    @Override
    public Flux<String> getAllGenres() {
        return Flux.fromIterable(genreNames);
    }

    private record Genre(String name, String slug) {}
}
