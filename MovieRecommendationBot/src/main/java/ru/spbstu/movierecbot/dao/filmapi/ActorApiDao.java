package ru.spbstu.movierecbot.dao.filmapi;

import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dto.ActorDto;

public interface ActorApiDao {
    // Получить актера по имени
    Mono<ActorDto> getActorByName(String actorName);
}
