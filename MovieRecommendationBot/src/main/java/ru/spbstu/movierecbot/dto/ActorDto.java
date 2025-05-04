package ru.spbstu.movierecbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActorDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("sex") String sex,
        @JsonProperty("growth") String growth,
        @JsonProperty("age") String age
) {}
