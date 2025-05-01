package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dao.filmapi.ActorApiDao;
import ru.spbstu.movierecbot.dto.ActorDto;

import java.util.List;

@Repository
public class ActorApiDaoImpl implements ActorApiDao {
    private final WebClient kinopoiskWebClient;

    public ActorApiDaoImpl(WebClient kinopoiskWebClient) {
        this.kinopoiskWebClient = kinopoiskWebClient;
    }


    @Override
    public Mono<ActorDto> getActorByName(String actorName) {
        return kinopoiskWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/person/search")
                        .queryParam("page", 1)
                        .queryParam("limit", 1)
                        .queryParam("query", actorName)
                        .build())
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(ApiResponseDto.class) // Десериализуем ответ в ApiResponseDto
                .flatMap(apiResponse -> {
                    if (apiResponse.actors() == null || apiResponse.actors().isEmpty()) {
                        return Mono.error(new RuntimeException("Актер не найден"));
                    }
                    return Mono.just(apiResponse.actors().getFirst());
                });
    }

    public record ApiResponseDto(@JsonProperty("docs") List<ActorDto> actors) {}
}
