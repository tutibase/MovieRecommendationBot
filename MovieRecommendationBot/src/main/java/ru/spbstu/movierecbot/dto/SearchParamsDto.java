package ru.spbstu.movierecbot.dto;

import java.util.List;

public record SearchParamsDto(
        List<Integer> years,
        List<String> ratings,
        String movieLength,
        List<String> genres,
        List<String> countries,
        List<ActorDto> actors
) {}