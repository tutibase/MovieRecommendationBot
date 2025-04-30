package ru.spbstu.movierecbot.dto;

import java.time.LocalDate;

public record UserDto(
        Long telegramId,
        LocalDate createdAt
) {}