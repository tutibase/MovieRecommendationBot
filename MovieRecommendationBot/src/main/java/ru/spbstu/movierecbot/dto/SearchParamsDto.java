package ru.spbstu.movierecbot.dto;

import java.util.List;

public record SearchParamsDto(
        List<String> years,
        List<String> ratings,
        List<String> movieLength,
        List<String> genres,
        List<String> countries,
        List<String> actors
) {}