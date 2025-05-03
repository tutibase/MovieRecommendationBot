package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dao.filmapi.GenreApiDao;

import java.util.Arrays;
import java.util.List;

@Repository
public class GenreApiDaoImpl implements GenreApiDao {
    private final List<String> genreNames;

    public GenreApiDaoImpl(ObjectMapper objectMapper) {
        String json = """
            [{"name":"аниме","slug":"anime"},
             {"name":"биография","slug":"biografiya"},
             {"name":"боевик","slug":"boevik"},
             {"name":"вестерн","slug":"vestern"},
             {"name":"военный","slug":"voennyy"},
             {"name":"детектив","slug":"detektiv"},
             {"name":"детский","slug":"detskiy"},
             {"name":"для взрослых","slug":"dlya-vzroslyh"},
             {"name":"документальный","slug":"dokumentalnyy"},
             {"name":"драма","slug":"drama"},
             {"name":"игра","slug":"igra"},
             {"name":"история","slug":"istoriya"},
             {"name":"комедия","slug":"komediya"},
             {"name":"концерт","slug":"koncert"},
             {"name":"короткометражка","slug":"korotkometrazhka"},
             {"name":"криминал","slug":"kriminal"},
             {"name":"мелодрама","slug":"melodrama"},
             {"name":"музыка","slug":"muzyka"},
             {"name":"мультфильм","slug":"multfilm"},
             {"name":"мюзикл","slug":"myuzikl"},
             {"name":"новости","slug":"novosti"},
             {"name":"приключения","slug":"priklyucheniya"},
             {"name":"реальное ТВ","slug":"realnoe-TV"},
             {"name":"семейный","slug":"semeynyy"},
             {"name":"спорт","slug":"sport"},
             {"name":"ток-шоу","slug":"tok-shou"},
             {"name":"триллер","slug":"triller"},
             {"name":"ужасы","slug":"uzhasy"},
             {"name":"фантастика","slug":"fantastika"},
             {"name":"фильм-нуар","slug":"film-nuar"},
             {"name":"фэнтези","slug":"fentezi"},
             {"name":"церемония","slug":"ceremoniya"}]""";

        try {
            Genre[] genres = objectMapper.readValue(json, Genre[].class);
            this.genreNames = Arrays.stream(genres)
                    .map(Genre::name)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга жанров", e);
        }
    }


    @Override
    public Flux<String> getAllGenres() {
        return Flux.fromIterable(genreNames);
    }

    private record Genre(String name, String slug) {}
}
