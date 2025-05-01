package ru.spbstu.movierecbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApiResponseDto(@JsonProperty("docs") List<FilmDto> films) {}