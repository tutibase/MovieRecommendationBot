package ru.spbstu.movierecbot.services;

import ch.qos.logback.core.joran.sanity.Pair;
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
        COUNTRY("country", "страны");

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
        List<String> availablePreferences;
        switch (type) {
            case GENRE:
                availablePreferences = genreApiDao.getAllGenres().collectList().block();
                break;
            case COUNTRY:
                availablePreferences = countryApiDao.getAllCountries().collectList().block();
                break;
            case YEAR:
                parsedInput.forEach(input -> {
                    // Пытаемся обработать как диапазон (формат: "2011-2014")
                    if (input.contains("-")) {
                        String[] years = input.split("-");
                        if (years.length == 2) {
                            try {
                                int startYear = Integer.parseInt(years[0].trim());
                                int endYear = Integer.parseInt(years[1].trim());

                                if (startYear > endYear) {
                                    checkResultLists.invalidInput.add(input);
                                    return;
                                }

                                boolean allValid = true;
                                List<String> validYears = new ArrayList<>();

                                for (int year = startYear; year <= endYear; year++) {
                                    if (year >= 1895 && year <= 2025) {
                                        validYears.add(String.valueOf(year));
                                    } else {
                                        allValid = false;
                                        break;
                                    }
                                }

                                if (allValid) {
                                    checkResultLists.validInput.addAll(validYears);
                                } else {
                                    checkResultLists.invalidInput.add(input);
                                }
                                return;
                            } catch (NumberFormatException e) {
                                // Не числа в диапазоне - проваливаемся в общую обработку ошибок
                            }
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
                });
                return checkResultLists;
            case ACTOR:
                parsedInput.forEach(input -> {
                    ActorDto actor = actorApiDao.getActorByName(input)
                            .onErrorResume(e -> Mono.empty()) // Преобразуем ошибку в пустой Mono
                            .block(); // Синхронно получаем результат

                    if (actor != null) {
                        // Добавляем имя актера и id из DTO (а не исходный input)
                        checkResultLists.validInputActors.add(Map.entry(actor.name(),actor.id()));
                    } else {
                        checkResultLists.invalidInput.add(input);
                    }
                });
                return checkResultLists;
            default:
                throw new IllegalArgumentException("Неизвестный тип предпочтения: " + type);
        }

        for (String input : parsedInput) {
            if (!availablePreferences.contains(input)) {
                checkResultLists.invalidInput.add(input);
            } else {
                checkResultLists.validInput.add(input);
            }
        }
        return checkResultLists;
    }
}
