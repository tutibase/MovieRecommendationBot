package ru.spbstu.movierecbot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dao.filmapi.ActorApiDao;
import ru.spbstu.movierecbot.dao.filmapi.CountryApiDao;
import ru.spbstu.movierecbot.dao.filmapi.GenreApiDao;
import ru.spbstu.movierecbot.dto.ActorDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CategoryCheckService {
    private final GenreApiDao genreApiDao;
    private final CountryApiDao countryApiDao;
    private final ActorApiDao actorApiDao;

    @Autowired
    public CategoryCheckService(GenreApiDao genreApiDao, CountryApiDao countryApiDao, ActorApiDao actorApiDao) {
        this.genreApiDao = genreApiDao;
        this.countryApiDao = countryApiDao;
        this.actorApiDao = actorApiDao;
    }


    public class CheckResultLists {
        //добавление
        List<String> validInput;
        List<Map.Entry<String, Integer>> validInputActors = new ArrayList<>(); //
        List<String> invalidInput;
        //только для работы с предпочтениями исп-тся следующие списки:
        List<String> duplicatePreferences;
        List<String> addedPreferences;
        //удаление
        List<String> nonexistentPreferences;
        List<String> deletedPreferences;


        public CheckResultLists() {
            this.validInput = new ArrayList<String>();

            this.invalidInput = new ArrayList<String>();
            this.duplicatePreferences = new ArrayList<String>();
            this.addedPreferences = new ArrayList<String>();
            this.nonexistentPreferences = new ArrayList<String>();
            this.deletedPreferences = new ArrayList<String>();
        }
    }

    public enum CategoryType{
        GENRE("genre", "жанры"),
        ACTOR("actor", "актеры"),
        YEAR("year", "годы"),
        COUNTRY("country", "страны"),
        RATE("rate", "рейтинги"),
        DURATION("duration", "длительности");


        private final String command;
        private final String displayName;

        CategoryType(String command, String displayName) {
            this.command = command;
            this.displayName = displayName;
        }

        public String getCommand() {
            return command;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static CategoryType fromCommand(String command) {
            for (CategoryType type : values()) {
                if (type.getCommand().equalsIgnoreCase(command)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Неизвестный тип предпочтений: " + command);
        }
    }

    public CheckResultLists checkIsInputValidByApi(CategoryType type, List<String> parsedInput) {
        CheckResultLists checkResultLists = new CheckResultLists();

        if (type == CategoryType.GENRE || type == CategoryType.COUNTRY) {
            List<String> availablePreferences = type == CategoryType.GENRE
                    ? genreApiDao.getAllGenres().collectList().block()
                    : countryApiDao.getAllCountries().collectList().block();

            parsedInput.forEach(input -> {
                if (availablePreferences.contains(input)) {
                    checkResultLists.validInput.add(input);
                } else {
                    checkResultLists.invalidInput.add(input);
                }
            });
            return checkResultLists;
        }

        if (type == CategoryType.DURATION){
            checkResultLists.invalidInput.addAll(parsedInput);
            return checkResultLists;
        }

        parsedInput.forEach(input -> {
            switch (type) {
                case YEAR -> validateYear(input, checkResultLists);
                case ACTOR -> validateActor(input, checkResultLists);
                case RATE -> validateRating(input, checkResultLists);
                case DURATION -> validateDuration(input, checkResultLists);
                default -> throw new IllegalArgumentException("Неизвестный тип: " + type);
            }
        });

        return checkResultLists;
    }

    // Методы валидации по категориям
    private void validateYear(String input, CheckResultLists checkResultLists) {
        // Обработка диапазона лет
        if (input.contains("-")) {
            String[] years = input.split("-");
            if (years.length == 2) {
                try {
                    int start = Integer.parseInt(years[0].trim());
                    int end = Integer.parseInt(years[1].trim());

                    if (start > end) {
                        checkResultLists.invalidInput.add(input);
                        return;
                    }

                    boolean allValid = true;
                    List<String> validYears = new ArrayList<>();
                    List<String> invalidYears = new ArrayList<>();

                    for (int year = start; year <= end; year++) {
                        if (year >= 1895 && year <= 2025) {
                            validYears.add(String.valueOf(year));
                        } else {
                            invalidYears.add(String.valueOf(year));
                        }
                    }
                    if (!validYears.isEmpty()){
                        checkResultLists.validInput.addAll(validYears);
                    }
                    if (!invalidYears.isEmpty()){
                        checkResultLists.invalidInput.addAll(invalidYears);
                    }
                } catch (NumberFormatException e) {
                    checkResultLists.invalidInput.add(input);
                }
                return;
            }
        }

        // Обработка одиночного года
        try {
            int year = Integer.parseInt(input);
            if (year >= 1895 && year <= 2025) {
                checkResultLists.validInput.add(input);
            } else {
                checkResultLists.invalidInput.add(input);
            }
        } catch (NumberFormatException e) {
            checkResultLists.invalidInput.add(input);
        }
    }

    private void validateActor(String input, CheckResultLists checkResultLists) {
        ActorDto actor = actorApiDao.getActorByName(input)
                .onErrorResume(e -> Mono.empty())
                .block();

        if (actor != null) {
            checkResultLists.validInputActors.add(Map.entry(actor.name(), actor.id()));
        } else {
            checkResultLists.invalidInput.add(input);
        }
    }

    private void validateRating(String input, CheckResultLists checkResultLists) {
        input = input.trim();

        // Обработка диапазона
        if (input.contains("-")) {
            String[] parts = input.split("-");
            if (parts.length == 2) {
                try {
                    double start = parseRating(parts[0].trim());
                    double end = parseRating(parts[1].trim());

                    if (isValidRating(start) && isValidRating(end) && start <= end) {
                        checkResultLists.validInput.add(input);
                    } else {
                        checkResultLists.invalidInput.add(input);
                    }
                } catch (NumberFormatException e) {
                    checkResultLists.invalidInput.add(input);
                }
                return;
            }
        }

        // Обработка одиночного числа
        try {
            double rating = parseRating(input);
            if (isValidRating(rating)) {
                checkResultLists.validInput.add(input);
            } else {
                checkResultLists.invalidInput.add(input);
            }
        } catch (NumberFormatException e) {
            checkResultLists.invalidInput.add(input);
        }
    }

    private void validateDuration(String input, CheckResultLists checkResultLists) {
        // Обработка только диапазона
        if (input.contains("-")) {
            String[] parts = input.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());

                    if (isValidDuration(start) && isValidDuration(end) && start <= end) {
                        checkResultLists.validInput.add(input);
                    } else {
                        checkResultLists.invalidInput.add(input);
                    }
                } catch (NumberFormatException e) {
                    checkResultLists.invalidInput.add(input);
                }
            }
        }
    }


    // Вспомогательные методы
    private double parseRating(String input) throws NumberFormatException {
        if (input.matches("^\\d+\\.\\d{1}$") || input.matches("^\\d+$")) {
            return Double.parseDouble(input);
        }
        throw new NumberFormatException();
    }

    private boolean isValidRating(double rating) {
        return rating >= 1.0 && rating <= 10.0;
    }

    private boolean isValidDuration(int minutes) {
        return minutes >= 0 && minutes <= 51420;
    }
}
