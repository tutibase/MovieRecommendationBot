package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dao.filmapi.CountryApiDao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Repository
public class CountryApiDaoImpl implements CountryApiDao {
    private final List<String> countries;

    @Override
    public Flux<String> getAllCountries() {
        return Flux.fromIterable(countries);
    }

    public CountryApiDaoImpl(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("countries.json");
        String json;

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] fileData = FileCopyUtils.copyToByteArray(inputStream);
            json = new String(fileData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка открытия json с странами", e);
        }

        try {
            CountryApiDaoImpl.Country[] genres = objectMapper.readValue(json, CountryApiDaoImpl.Country[].class);
            this.countries = Arrays.stream(genres)
                    .map(CountryApiDaoImpl.Country::name)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга стран", e);
        }
    }

    private record Country(String name, String slug) {}
}
