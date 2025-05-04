package ru.spbstu.movierecbot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dao.filmapi.ActorApiDao;
import ru.spbstu.movierecbot.dao.filmapi.CountryApiDao;
import ru.spbstu.movierecbot.dao.filmapi.GenreApiDao;
import ru.spbstu.movierecbot.dao.pg.*;
import ru.spbstu.movierecbot.dbClasses.tables.records.ActorsRecord;
import ru.spbstu.movierecbot.dbClasses.tables.records.CountriesRecord;
import ru.spbstu.movierecbot.dbClasses.tables.records.GenresRecord;
import ru.spbstu.movierecbot.dbClasses.tables.records.YearsRecord;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import static ru.spbstu.movierecbot.services.CategoryCheckService.CategoryType;
import  ru.spbstu.movierecbot.services.CategoryCheckService.CheckResultLists;
import java.util.Arrays;
import java.util.List;


@Service
public class PreferencesService {
    private final ActorsDao actorsDao;
    private final CountriesDao countriesDao;
    private final GenreDao genreDao;
    private final YearDao yearDao;
    private final CategoryCheckService categoryCheckService;
    final int PREFERENCE_ALREADY_EXISTS = 0;
    final int PREFERENCE_ADDED = 1;
    final int PREFERENCE_NOT_EXISTS = 0;
    final int PREFERENCE_DELETED = 1;

    @Autowired
    public PreferencesService(ActorsDao actorsDao, CountriesDao countriesDao, GenreDao genreDao, YearDao yearDao, GenreApiDao genreApiDao, CountryApiDao countryApiDao, ActorApiDao actorApiDao, CategoryCheckService categoryCheckService) {
        this.actorsDao = actorsDao;
        this.countriesDao = countriesDao;
        this.genreDao = genreDao;
        this.yearDao = yearDao;
        this.categoryCheckService = categoryCheckService;
    }


    record PreferenceResult(String value, int status) {}

    // Метод для показа списка предпочтений пользователя
    public Mono<String> showPreferences(long telegramId) {
        // Оборачиваем синхронные DAO-вызовы и указываем scheduler
        // Получаем предпочтения
        Mono<List<ActorsRecord>> actorsMono = Mono.fromCallable(() -> actorsDao.getActorsByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic());

        Mono<List<CountriesRecord>> countriesMono = Mono.fromCallable(() -> countriesDao.getCountriesByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic());

        Mono<List<GenresRecord>> genresMono = Mono.fromCallable(() -> genreDao.getGenresByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic());

        Mono<List<YearsRecord>> yearsMono = Mono.fromCallable(() -> yearDao.getYearsByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic());

        // Комбинируем и обрабатываем результат
        return Mono.zip(actorsMono, countriesMono, genresMono, yearsMono)
                .map(tuple -> {
                    List<ActorsRecord> actors = tuple.getT1();
                    List<CountriesRecord> countries = tuple.getT2();
                    List<GenresRecord> genres = tuple.getT3();
                    List<YearsRecord> years = tuple.getT4();

                    // Формируем результат, если есть предпочтения
                    StringBuilder result = new StringBuilder();

                    // Проверяем наличие предпочтений
                    // Нет предпочтений вообще
                    if (actors.isEmpty() && countries.isEmpty() && genres.isEmpty() && years.isEmpty()) {
                        return "У вас пока нет предпочтений. Добавьте их с помощью команды /addPref.";
                    }

                    result.append("⭐ Ваши предпочтения ⭐\n\n");

                    if (!actors.isEmpty()) {
                        result.append("🎭 Актеры:\n");
                        actors.forEach(a -> result.append("   ▸ ").append(a.getFullName()).append("\n"));
                        result.append("\n");
                    }

                    if (!countries.isEmpty()) {
                        result.append("🌍 Страны:\n");
                        countries.forEach(c -> result.append("   ▸ ").append(c.getName()).append("\n"));
                        result.append("\n");
                    }

                    if (!genres.isEmpty()) {
                        result.append("🎬 Жанры:\n");
                        genres.forEach(g -> result.append("   ▸ ").append(g.getName()).append("\n"));
                        result.append("\n");
                    }

                    if (!years.isEmpty()) {
                        result.append("📅 Годы:\n");
                        years.forEach(y -> result.append("   ▸ ").append(y.getValue()).append("\n"));
                        result.append("\n");
                    }

                    return result.toString();
                });
    }


    // Общий метод для добавления предпочтений
    public Mono<String> addPreferences(long telegramId, String input, String preferenceCommand) {
        CategoryType type = CategoryType.fromCommand(preferenceCommand);

        // Парсим ввод пользователя по запятым
        List<String> parsedInput = Arrays.stream(input.split(","))
                .map(String::trim)
                .distinct()  // Удаляет дубликаты
                .toList();

        if (parsedInput.isEmpty()) {
            return Mono.just("Вы не ввели никаких предпочтений для добавления.");
        }

        // Реактивная проверка валидности предпочтений
        return Mono.fromCallable(() -> categoryCheckService.checkIsInputValidByApi(type, parsedInput))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(checkResultLists -> {
                    if (checkResultLists.validInput.isEmpty() &&
                            (type != CategoryType.ACTOR || checkResultLists.validInputActors.isEmpty())) {
                        return Mono.just(buildResultAddMessage(type, checkResultLists));
                    }

                    // Обработка для актёров (если тип ACTOR и есть validInputActors)
                    if (type == CategoryType.ACTOR) {
                        return Flux.fromIterable(checkResultLists.validInputActors)
                                .flatMap(pair -> Mono.fromCallable(() ->
                                        new PreferenceResult(
                                                pair.getValue(),
                                                addPreferenceToUserDb(telegramId, pair.getValue(), Integer.valueOf(pair.getKey()), type)
                                        )).subscribeOn(Schedulers.boundedElastic())
                                )
                                .collectList()
                                .flatMap(results -> processValidInput(results, checkResultLists, type));
                    }
                    // Обработка для остальных типов
                    else {
                        Integer fakeId = 0;
                        return Flux.fromIterable(checkResultLists.validInput)
                                .flatMap(pref -> Mono.fromCallable(() ->
                                        new PreferenceResult(
                                                pref,
                                                addPreferenceToUserDb(telegramId, pref, fakeId, type)
                                        )).subscribeOn(Schedulers.boundedElastic())
                                )
                                .collectList()
                                .flatMap(results -> processValidInput(results, checkResultLists, type));
                    }
                });
    }


    // Метод для сортировки валидного ввода на успешно добавленное и дубликаты
    private Mono<String> processValidInput(
            List<PreferenceResult> results,
            CheckResultLists checkResultLists,
            CategoryType type
    ) {
        results.forEach(r -> {
            switch (r.status()) {
                case PREFERENCE_ADDED -> checkResultLists.addedPreferences.add(r.value());
                case PREFERENCE_ALREADY_EXISTS -> checkResultLists.duplicatePreferences.add(r.value());
            }
        });
        return Mono.just(buildResultAddMessage(type, checkResultLists));
    }


    // Метод для формирования итогового сообщения после добавления предпочтений
    private String buildResultAddMessage(CategoryType type, CheckResultLists checkResultLists) {
        StringBuilder result = new StringBuilder();
        String category = type.getDisplayName().toLowerCase(); // "жанры", "режиссеры" и т.д.

        if (!checkResultLists.addedPreferences.isEmpty()) {
            result.append("✨ Отлично! Новые ").append(category).append(" добавлены в список предпочтений: \n")
                    .append("👉 ").append(String.join(", ", checkResultLists.addedPreferences))
                    .append("\n\n");
        }

        if (!checkResultLists.duplicatePreferences.isEmpty()) {
            result.append("😊 Эти ").append(category).append(" уже есть в вашем списке предпочтений: \n")
                    .append("🔹 ").append(String.join(", ", checkResultLists.duplicatePreferences))
                    .append("\n\n");
        }

        if (!checkResultLists.invalidInput.isEmpty()) {
            result.append("🤔 К сожалению, эти ").append(category)
                    .append(" не добавлены в список предпочтений (проверьте написание): \n")
                    .append("✖ ").append(String.join(", ", checkResultLists.invalidInput))
                    .append("\n\n");
        }

        return result.toString().trim();
    }

    // Метод для добавления предпочтения в БД пользователей (в зависимости от типа)
    private int addPreferenceToUserDb(long telegramId, String preference, Integer idActor, CategoryType type) {
        return switch (type) {
            case GENRE -> genreDao.addGenre(telegramId, preference.toLowerCase());
            case ACTOR -> actorsDao.addActor(telegramId, idActor, preference);
            case YEAR -> yearDao.addYear(telegramId, Integer.parseInt(preference));
            case COUNTRY -> countriesDao.addCountry(telegramId, preference);
            default -> throw new IllegalArgumentException("Неизвестный тип предпочтений: " + type);
        };
    }


    // Общий метод для удаления предпочтений
    public Mono<String> deletePreferences(long telegramId, String input, String preferenceCommand) {
        CategoryType type = CategoryType.fromCommand(preferenceCommand);
        CheckResultLists checkResultLists = categoryCheckService.new CheckResultLists();

        // Парсим ввод (синхронная операция)
        List<String> parsedInput = Arrays.stream(input.split(","))
                .map(String::trim)
                .toList();

        if (parsedInput.isEmpty()) {
            return Mono.just("Вы не ввели никаких предпочтений для удаления.");
        }

        // Реактивное удаление предпочтений
        return Flux.fromIterable(parsedInput)
                .flatMap(pref -> Mono.fromCallable(() ->
                                        new PreferenceResult(pref, deletePreferenceFromUserDb(telegramId, pref, type))
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .collectList()
                .map(results -> {
                    // Сортировка валидного ввода на успешно удаленное и не существующее в БД пользователей
                    results.forEach(r -> {
                        switch (r.status()) {
                            case PREFERENCE_DELETED -> checkResultLists.deletedPreferences.add(r.value());
                            case PREFERENCE_NOT_EXISTS -> checkResultLists.nonexistentPreferences.add(r.value());
                        }
                    });

                    // Формируем итоговое сообщение
                    return buildDeleteResultMessage(type, checkResultLists);
                });
    }


    // Метод для формирования итогового сообщения после удаления предпочтений
    private String buildDeleteResultMessage(CategoryType type, CheckResultLists checkResultLists) {
        StringBuilder result = new StringBuilder();
        String category = type.getDisplayName().toLowerCase(); // "жанры", "режиссеры" и т.д.

        if (!checkResultLists.deletedPreferences.isEmpty()) {
            result.append("✅ Готово! Эти ").append(category).append(" удалены из списка предпочтений: \n")
                    .append("🗑️ ").append(String.join(", ", checkResultLists.deletedPreferences))
                    .append("\n\n");
        }

        if (!checkResultLists.nonexistentPreferences.isEmpty()) {
            result.append("ℹ️ Эти ").append(category).append(" не удалось удалить, их нет в списке предпочтений: \n")
                    .append("🔍 ").append(String.join(", ", checkResultLists.nonexistentPreferences))
                    .append("\n\n");
        }

        return result.toString().trim();
    }


    // Метод для удаления предпочтения из БД пользователей (в зависимости от типа)
    private int deletePreferenceFromUserDb(long telegramId, String preference, CategoryType type) {
        return switch (type) {
            case GENRE -> genreDao.deleteGenre(telegramId, preference.toLowerCase());
            case ACTOR -> actorsDao.deleteActor(telegramId, preference);
            case YEAR -> yearDao.deleteYear(telegramId, Integer.parseInt(preference));
            case COUNTRY -> countriesDao.deleteCountry(telegramId, preference);
            default -> throw new IllegalArgumentException("Неизвестный тип предпочтений: " + type);
        };
    }
}
