package ru.spbstu.movierecbot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dao.pg.ActorsDao;
import ru.spbstu.movierecbot.dao.pg.CountriesDao;
import ru.spbstu.movierecbot.dao.pg.GenreDao;
import ru.spbstu.movierecbot.dao.pg.YearDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.CountriesRecord;
import ru.spbstu.movierecbot.dbClasses.tables.records.GenresRecord;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.spbstu.movierecbot.services.CategoryCheckService.CategoryType;
import ru.spbstu.movierecbot.dto.SearchParamsDto;
import ru.spbstu.movierecbot.services.CategoryCheckService.CheckResultLists;

@Service
public class SearchFilmService {
    private static final Logger log = LoggerFactory.getLogger(SearchFilmService.class);
    private final ConcurrentHashMap<Long, HashMap<CategoryCheckService.CategoryType, List<Map.Entry<String, String>>>> usersFilterInput;
    private final FilmDao filmDao;
    private final ActorsDao actorsDao;
    private final CountriesDao countriesDao;
    private final GenreDao genreDao;
    private final YearDao yearDao;
    private final InfoAboutFilmService infoAboutFilmService;
    private final CategoryCheckService categoryCheckService;

    @Autowired
    public SearchFilmService(FilmDao filmDao, ActorsDao actorsDao, CountriesDao countriesDao, GenreDao genreDao, YearDao yearDao, CategoryCheckService categoryCheckService, InfoAboutFilmService infoAboutFilmService) {
        this.usersFilterInput = new ConcurrentHashMap<>();
        this.filmDao = filmDao;
        this.actorsDao = actorsDao;
        this.countriesDao = countriesDao;
        this.genreDao = genreDao;
        this.yearDao = yearDao;
        this.categoryCheckService = categoryCheckService;
        this.infoAboutFilmService = infoAboutFilmService;
    }


    // Метод для поиска случайного фильма
    public Mono<String> searchRandomFilm(){
        return filmDao.getRandomFilm().flatMap(filmDto -> {
            String formattedInfo = infoAboutFilmService.formatFilmDetails(filmDto);
            return Mono.just(formattedInfo);
        }).onErrorResume(error -> Mono.just("Ошибка при поиске фильма: "));
    }

    // Метод для поиска фильма по предпочтениям
    public Mono<String> searchFilmByPreferences(long telegramId){

        // Получаем предпочтения
        Mono<List<String>> actorsMono = Mono.fromCallable(() -> actorsDao.getActorsByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(actors -> actors.stream().map(actor->actor.getActorId().toString()).collect(Collectors.toList()));

        Mono<List<String>> countriesMono = Mono.fromCallable(() -> countriesDao.getCountriesByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(countries -> countries.stream().map(CountriesRecord::getName).collect(Collectors.toList()));

        Mono<List<String>> genresMono = Mono.fromCallable(() -> genreDao.getGenresByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(genres -> genres.stream().map(GenresRecord::getName).collect(Collectors.toList()));

        Mono<List<String>> yearsMono = Mono.fromCallable(() -> yearDao.getYearsByTelegramId(telegramId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(years -> years.stream().map(year -> year.getValue().toString()).collect(Collectors.toList()));


        return Mono.zip(actorsMono, countriesMono, genresMono, yearsMono)
                .flatMap(tuple -> {
                    List<String> actors = tuple.getT1();
                    List<String> countries = tuple.getT2();
                    List<String> genres = tuple.getT3();
                    List<String> years = tuple.getT4();

                    StringBuilder result = new StringBuilder();

                    if (actors.isEmpty() && years.isEmpty() && genres.isEmpty() && countries.isEmpty()) {
                        result.append("🤔 У вас нет предпочтений, поиск невозможен.");
                        return Mono.just(result.toString());
                    }

                    // Создаем DTO с параметрами поиска
                    SearchParamsDto searchParamsDto = new SearchParamsDto(
                            years,
                            List.of(),
                            List.of(),
                            genres,
                            countries,
                            actors
                    );

                    //System.out.println(searchParamsDto);
                    return filmDao.getFilmsByParams(searchParamsDto)
                            .collectList()
                            .flatMap(films -> {
                                if (films.isEmpty()) {
                                    result.append("🤔 Не найдено ни одного фильма, подходящего под ваши предпочтения.");
                                } else {
                                    result.append("✨ Вот найденные фильмы по вашим предпочтениям:\n\n");
                                    films.forEach(filmDto -> {
                                        String formattedInfo = infoAboutFilmService.formatFilmDetails(filmDto);
                                        result.append(formattedInfo).append("---CUTHERESPLITTER---");
                                    });
                                }
                                return Mono.just(result.toString());
                            }).onErrorResume(error -> Mono.just("⚠️ Произошла ошибка при поиске фильмов по вашим предпочтениям."));
                });
    }


    // Метод для поиска фильма по фильтрам
    public Mono<String> addSearchFilter(long telegramId, String input, String preferenceCommand) {
        CategoryType type = CategoryType.fromCommand(preferenceCommand);

        // Парсим ввод пользователя по запятым
        List<String> parsedInput = Arrays.stream(input.split(","))
                .map(String::trim)
                .distinct()  // Удаляет дубликаты
                .toList();

        // кажется это лишняя проверка
        if (parsedInput.isEmpty()) {
            return Mono.just("😒 Вы не ввели никаких фильтров для добавления.");
        }

        // Проверка валидности ввода
        CheckResultLists checkResultLists = categoryCheckService.checkIsInputValidByApi(type, parsedInput);


        List<Map.Entry<String, String>> mappedValidInput = checkResultLists.validInput.stream().map(s -> Map.entry(s, ""))
                .toList();

        // Сохраняем валидный ввод в ConcurrentHashMap для дальнейшего использования
        usersFilterInput.compute(telegramId, (key, userMap) -> {
            if (userMap == null) {
                userMap = new HashMap<>();
            }
            if (type == CategoryType.ACTOR) {
                userMap.put(type, new ArrayList<Map.Entry<String,String>>(checkResultLists.validInputActors));
            } else {
                userMap.put(type, new ArrayList<Map.Entry<String,String>>(mappedValidInput));
            }
            return userMap;
        });
        return Mono.just(buildFilterAddResultMessage(type, checkResultLists));
    }


    // Метод для вывода результата добавления фильтров
    private String buildFilterAddResultMessage(CategoryType type, CheckResultLists checkResultLists) {
        StringBuilder result = new StringBuilder();
        String category = type.getDisplayName().toLowerCase(); // "жанры", "режиссеры" и т.д.

        if (!checkResultLists.validInput.isEmpty()) {
            result.append("✨ Отлично! Эти ").append(category).append(" добавлены в фильтр: \n")
                    .append("👉 ").append(String.join(", ", checkResultLists.validInput))
                    .append("\n\n");
        }

        if (!checkResultLists.validInputActors.isEmpty() && type == CategoryType.ACTOR) {
            List<String> actorNameStrings = checkResultLists.validInputActors.stream()
                    .map(entry -> entry.getValue())
                    .toList();
            result.append("✨ Отлично! Эти ").append(category).append(" добавлены в фильтр: \n")
                    .append("👉 ").append(String.join(", ", actorNameStrings))
                    .append("\n\n");
        }

        if (!checkResultLists.invalidInput.isEmpty()) {
            result.append("🤔 К сожалению, эти ").append(category).append(" не добавлены в фильтр (проверьте написание) : \n")
                    .append("🔹 ").append(String.join(", ", checkResultLists.invalidInput))
                    .append("\n\n");
        }
        return result.toString().trim();
    }

    // Метод для применения фильтров и поиска фильма по ним
    public Mono<String> applySearchFilters(long telegramId){
        StringBuilder result = new StringBuilder();

        if (usersFilterInput.get(telegramId) == null){
            result.append("😒 Вы не ввели ни одного фильтра, поиск невозможен.");
            return Mono.just(result.toString());
        }

        List<String> actors = getOneFilterListFromCommonList(telegramId,CategoryType.ACTOR);
        List<String> years = getOneFilterListFromCommonList(telegramId,CategoryType.YEAR);
        List<String> ratings = getOneFilterListFromCommonList(telegramId,CategoryType.RATE);
        List<String> genres = getOneFilterListFromCommonList(telegramId,CategoryType.GENRE);
        List<String> duration = getOneFilterListFromCommonList(telegramId,CategoryType.DURATION);
        List<String> countries = getOneFilterListFromCommonList(telegramId,CategoryType.COUNTRY);


        if (actors.isEmpty() && years.isEmpty() && ratings.isEmpty() && genres.isEmpty() && duration.isEmpty() && countries.isEmpty()){
            result.append("😒 Вы не ввели ни одного фильтра, поиск невозможен.");
            return Mono.just(result.toString());
        }
        SearchParamsDto searchParamsDto = new SearchParamsDto(years, ratings, duration, genres, countries, actors);

        //log.info(actors.toString() + "herehehe");
        return filmDao.getFilmsByParams(searchParamsDto)
                .collectList()
                .flatMap(films -> {
                    // Очищаем фильтры ПОСЛЕ успешного поиска
                    usersFilterInput.remove(telegramId);
                    if (films.isEmpty()) {
                        result.append("🤔 Не найдено ни одного фильма, подходящего под ваши фильтры поиска.");
                    } else {
                        //result.append("✨ Вот найденные фильмы по вашим фильтрам:\n\n");
                        films.forEach(filmDto -> {
                            String formattedInfo = infoAboutFilmService.formatFilmDetails(filmDto);
                            result.append(formattedInfo).append("---CUTHERESPLITTER---");
                        });
                    }
                    return Mono.just(result.toString());
                })
                .onErrorResume(error -> {
                    // Очищаем фильтры ПОСЛЕ успешного поиска
                    usersFilterInput.remove(telegramId);
                    return Mono.just("🤔 Не найдено ни одного фильма, подходящего под ваши фильтры поиска.");
                });
    }


    private List<String> getOneFilterListFromCommonList(long telegramId, CategoryType categoryType){
        return usersFilterInput.get(telegramId)
                .getOrDefault(categoryType, List.of())
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

    }


    public Mono<String> showFilters(long telegramId){
        StringBuilder result = new StringBuilder();

        if (usersFilterInput.get(telegramId) == null){
            result.append("😒 У вас нет добавленных фильтров.");
            return Mono.just(result.toString());
        }

        List<String> actors = usersFilterInput.get(telegramId)
                .getOrDefault(CategoryType.ACTOR, List.of())
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        List<String> years = getOneFilterListFromCommonList(telegramId,CategoryType.YEAR);
        List<String> ratings = getOneFilterListFromCommonList(telegramId,CategoryType.RATE);
        List<String> genres = getOneFilterListFromCommonList(telegramId,CategoryType.GENRE);
        List<String> duration = getOneFilterListFromCommonList(telegramId,CategoryType.DURATION);
        List<String> countries = getOneFilterListFromCommonList(telegramId,CategoryType.COUNTRY);

        result.append("✨ Ваши текущие фильтры:\n\n");

        if (!years.isEmpty()) {
            result.append("📅 Годы: ").append(String.join(", ", years)).append("\n");
        }

        if (!ratings.isEmpty()) {
            result.append("⭐ Рейтинги: ").append(String.join(", ", ratings)).append("\n");
        }

        if (!duration.isEmpty()) {
            result.append("⏳ Длительность: ").append(String.join(", ", duration)).append("\n");
        }

        if (!genres.isEmpty()) {
            result.append("🎭 Жанры: ").append(String.join(", ", genres)).append("\n");
        }

        if (!countries.isEmpty()) {
            result.append("🌍 Страны: ").append(String.join(", ", countries)).append("\n");
        }

        if (!actors.isEmpty()) {
            result.append("🎬 Актёры: ").append(String.join(", ", actors)).append("\n");
        }

        return Mono.just(result.toString());
    }
}
