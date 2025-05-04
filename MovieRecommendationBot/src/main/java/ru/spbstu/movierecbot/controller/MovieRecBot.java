package ru.spbstu.movierecbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.spbstu.movierecbot.services.*;
import ru.spbstu.movierecbot.services.state.StateService;
import ru.spbstu.movierecbot.services.state.UserState;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class MovieRecBot extends TelegramLongPollingBot implements TelegramBot {


    private final String botUsername;

    private final InfoAboutFilmService infoAboutFilmServiceService;
    private final UserService userService;
    private final WatchListService watchListService;
    private final WatchedListService watchedListService;
    private final StateService stateService;
    private final MenuService menuService;
    private final PreferencesService preferencesService;
    private final SearchFilmService searchFilmService;
    private final KeyboardService keyboardService;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupMenu;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupFilters;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupSearch;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupWatchList;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupPreferences;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupShowPeriod;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupDeletePreferences;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupWatchedList;
    Mono<ReplyKeyboardMarkup> replyKeyboardMarkupAddPreferences;


    @Autowired
    public MovieRecBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.username}") String botUsername, InfoAboutFilmService service, UserService userService, WatchListService watchListService, WatchedListService watchedListService, StateService stateService, MenuService menuService, PreferencesService preferencesService, SearchFilmService searchFilmService, KeyboardService keyboardService
    ) {
        super(botToken);
        this.botUsername = botUsername;
        this.infoAboutFilmServiceService = service;
        this.userService = userService;
        this.watchListService = watchListService;
        this.watchedListService = watchedListService;
        this.stateService = stateService;
        this.menuService = menuService;
        this.preferencesService = preferencesService;
        this.searchFilmService = searchFilmService;
        this.keyboardService = keyboardService;
        this.replyKeyboardMarkupMenu = keyboardService.menuKeyboardMarkup();
        this.replyKeyboardMarkupFilters = keyboardService.getFiltersKeyboard();
        this.replyKeyboardMarkupSearch = keyboardService.searchKeyboardMarkup();
        this.replyKeyboardMarkupWatchList = keyboardService.watchListKeyboardMarkup();
        this.replyKeyboardMarkupPreferences = keyboardService.preferencesKeyboardMarkup();
        this.replyKeyboardMarkupShowPeriod = keyboardService.showWatchedPeriodKeyboardMarkup();
        this.replyKeyboardMarkupDeletePreferences = keyboardService.deletePreferencesKeyboardMarkup();
        this.replyKeyboardMarkupWatchedList = keyboardService.watchedListKeyboardMarkup();
        this.replyKeyboardMarkupAddPreferences = keyboardService.addPreferencesKeyboardMarkup();

    }



    @PostConstruct
    private void init() {
        initializeBotCommands();
    }

    private void initializeBotCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("menu", "Главное меню"));
        commands.add(new BotCommand("searchfilm", "Поиск фильмов"));
        commands.add(new BotCommand("watchlist", "Буду смотреть"));
        commands.add(new BotCommand("watchedlist", "Просмотренные фильмы"));
        commands.add(new BotCommand("preferences", "Мои предпочтения"));
        commands.add(new BotCommand("infoaboutfilm", "Информация о фильме"));

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при установке команд: " + e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        String command = update.getMessage().getText();
        String firstName = update.getMessage().getFrom().getFirstName() == null ? "" : update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName() == null ? "" : update.getMessage().getFrom().getFirstName();
        String username = (firstName + " " + lastName).trim();
        keyboardService.getCommandFromKeyboard(command).subscribeOn(Schedulers.boundedElastic()).subscribe(
                result -> {
                    String finalCommand = !result.equals("Неизвестный текст с клавиатуры.")
                            ? result
                            : command;
                    if (finalCommand.equals("/start")||finalCommand.equals("/menu")||finalCommand.equals("/searchfilm")
                            || finalCommand.equals("/watchlist")|| finalCommand.equals("/preferences") || finalCommand.equals("/watchedlist")
                            || finalCommand.equals("/infoaboutfilm")){
                        handleMainCommand(finalCommand, update.getMessage().getChatId(),
                                username);
                    }
                    else{
                        handleStatefulInput(finalCommand, update.getMessage().getChatId(), username);
                    }
                }
        );
    }

    private void handleMainCommand(String command, Long chatId, String username){
        StringBuilder response = new StringBuilder();
        switch (command) {
            case "/start":
                userService.registerUser(chatId);
                response.append("Добро пожаловать в MovieRecBot, ").append(username).append("!\n\n");
                menuService.showMainMenu().subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupMenu);
                                    stateService.setState(chatId, UserState.IDLE);
                                }
                        );
                break;
            case "/menu":
                menuService.showMainMenu().subscribeOn(Schedulers.boundedElastic())
                                .subscribe(
                                        result -> {
                                            response.append(result);
                                            sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupMenu);
                                            stateService.setState(chatId, UserState.IDLE);
                                        }
                                );
                break;
            case "/searchfilm":
                response.append("""
                        🔍 <b>Выберите способ поиска фильмов:</b>
                        
                        🎛️ A. /searchByFilters - Поиск по фильтрам
                        ❤️ B. /searchByPref - Поиск по вашим предпочтениям
                        🎲 C. /searchRandom - Случайная рекомендация
                        
                        Какой вариант вам интересен? 😊""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupSearch);
                stateService.setState(chatId, UserState.WAITING_SEARCH_COMMAND);
                break;
            case "/watchlist":
                response.append("""
                        \uD83D\uDCCB <b>Работа со списком «Буду смотреть»</b>
                        
                        Выберите действие или команду:
                        \uD83D\uDC41\uFE0F /showWatchList — Показать список
                        ➕ /addToWatchList — Добавить фильм
                        \uD83D\uDDD1\uFE0F /deleteFromWatchList — Удалить фильм
                        
                        Управляйте вашей коллекцией легко! \uD83D\uDE09""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchList);
                stateService.setState(chatId, UserState.WAITING_WATCH_LIST_COMMAND);
                break;
            case "/preferences":
                response.append("""
                        \uD83C\uDF1F <b>Работа с вашими предпочтениями</b> \uD83C\uDF1F
                        
                        Выберите действие:
                        
                        \uD83D\uDD0D /showMyPreferences - Показать текущие предпочтения
                        ➕ /addPreferences - Добавить новые предпочтения
                        ❌ /deletePreferences - Удалить предпочтения
                        
                        Ваш выбор поможет нам сделать подборку идеальной! \uD83D\uDCAB""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupPreferences);
                stateService.setState(chatId, UserState.WAITING_PREFERENCE_COMMAND);
                break;
            case "/watchedlist":
                response.append("""
                        \uD83C\uDFAC <b>Работа со списком "Просмотренные фильмы"</b> \uD83C\uDFAC
                        
                        Выберите действие:
                        
                        \uD83D\uDCCB /showWatchedFilmsList - Показать просмотренные фильмы
                        ➕ /addToWatchedFilmsList - Добавить фильм в список
                        ⭐ /addMarkToWatchedFilm - Оценить фильм
                        ✏\uFE0F /addReviewToWatchedFilm - Написать отзыв
                        
                         Сохраняйте ваши киновпечатления! \uD83C\uDF7F""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchedList);
                stateService.setState(chatId, UserState.WAITING_WATCHED_LIST_COMMAND);
                break;
            case "/infoaboutfilm":
                response.append("\uD83D\uDD0D Введите название фильма, о котором хотите получить информацию:");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupMenu);
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_INF);
                break;
            default:
                response.append("🤷‍♂️ Ой! Такой команды нет. Чтобы узнать список доступных команд, воспользуйся:\n" +
                        "/menu - список доступных команд");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupMenu);
        }
    }
    private void handleCommand(String command, Long chatId) {
        StringBuilder response = new StringBuilder();


        switch (command) {
            case "/addToWatchList":
                sendResponse(chatId, "📋 Отлично! Какой фильм добавим в ваш список \"Буду смотреть\"?");
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_TO_WATCH_LIST);
                break;
            case "/deleteFromWatchList":
                sendResponse(chatId, "🗑️ Введите название фильма для удаления из списка \"Буду смотреть\":");
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_DELETE_FROM_WATCH_LIST);
                break;
            case "/showWatchList":
                watchListService.showWatchList(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchList);
                                    stateService.setState(chatId, UserState.WAITING_WATCH_LIST_COMMAND);
                                }
                        );
                break;
            case "/addPreferences":
                response.append("""
                        🌟 <b>Добавление предпочтений</b> 🌟
                        
                        Выберите категорию для настройки:
                        
                        🎭 /addGenrePreferences - Любимые жанры
                        👨‍🎤 /addActorPreferences - Предпочитаемые актеры
                        🌍 /addCountryPreferences - Страны производства
                        📅 /addYearPreferences - Годы выпуска
                        
                        Настройте подборку под свой вкус! 💫""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                stateService.setState(chatId, UserState.WAITING_PREF_TYPE_FOR_ADD);
                break;
            case "/addGenrePreferences":
                sendResponse(chatId, "📋 Введите любимые жанры через запятую:");
                stateService.setState(chatId, UserState.WAITING_GENRE_FOR_ADD);
                break;
            case "/addActorPreferences":
                sendResponse(chatId, "👨‍🎤 Введите актеров через запятую:");
                stateService.setState(chatId, UserState.WAITING_ACTOR_FOR_ADD);
                break;
            case "/addCountryPreferences":
                sendResponse(chatId, "🗺️ Введите страны через запятую:");
                stateService.setState(chatId, UserState.WAITING_COUNTRY_FOR_ADD);
                break;
            case "/addYearPreferences":
                sendResponse(chatId, """
                        ⏳ Введите годы выпуска фильмов:
                        
                        • Диапазон:  <b>2005-2010</b>
                        • Отдельные годы: <b>2012, 2015, 2018</b>
                        
                        Выберите удобный формат ввода 🎬""");
                stateService.setState(chatId, UserState.WAITING_YEAR_FOR_ADD);
                break;
            case "/deletePreferences":
                response.append("""
                        🗑️ <b>Удаление предпочтений</b> 🗑️
                        
                        Выберите категорию для удаления:
                        
                        🎭 /deleteGenrePreferences - Удалить жанры
                        👨‍🎤 /deleteActorPreferences - Удалить актеров
                        🌍 /deleteCountryPreferences - Удалить страны
                        📅 /deleteYearPreferences - Удалить годы
                        
                        Вы можете удалить ненужные предпочтения ❌""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                stateService.setState(chatId, UserState.WAITING_PREF_TYPE_FOR_DELETE);
                break;
            case "/deleteGenrePreferences":
                sendResponse(chatId, "✂️ Введите жанры для удаления через запятую:");
                stateService.setState(chatId, UserState.WAITING_GENRE_FOR_DELETE);
                break;
            case "/deleteActorPreferences":
                sendResponse(chatId, "✂️ Введите имена актеров для удаления через запятую:");
                stateService.setState(chatId, UserState.WAITING_ACTOR_FOR_DELETE);
                break;
            case "/deleteCountryPreferences":
                sendResponse(chatId, "✂️ Введите страны для удаления через запятую:");
                stateService.setState(chatId, UserState.WAITING_COUNTRY_FOR_DELETE);
                break;
            case "/deleteYearPreferences":
                sendResponse(chatId, """
                        🗓️ <b>Удаление годов из предпочтений</b> 🗓️
                        
                        Введите:
                        • Диапазон: <b>2005-2010</b>
                        • Или отдельные годы: <b>2012, 2015, 2018</b>
                        
                        ❌ Указанные годы будут удалены из ваших предпочтений.""");
                stateService.setState(chatId, UserState.WAITING_YEAR_FOR_DELETE);
                break;
            case "/showMyPreferences":
                preferencesService.showPreferences(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupPreferences);
                                    stateService.setState(chatId, UserState.WAITING_PREFERENCE_COMMAND);
                                }
                        );
                break;
            case "/addToWatchedFilmsList":
                sendResponse(chatId, "🍿 Какой фильм вы посмотрели?\n" +
                        "Добавьте его в список \"Просмотренные\", указав точное название:");
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_TO_WATCHED_LIST);
                break;
            case "/addMarkToWatchedFilm":
                sendResponse(chatId, "⭐ Введите название фильма для оценки:");
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_MARK);
                break;
            case "/addReviewToWatchedFilm":
                sendResponse(chatId, "✏️ Введите название фильма для написания отзыва:");
                stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_REVIEW);
                break;
            case "/showWatchedFilmsList":
                response.append("""
                        📅 <b>Выберите период для просмотра истории:</b>
                        
                        ⏳ /threeMonths - Последние 3 месяца
                        🗓️ /lastMonth - Прошлый месяц
                        🎉 /lastYear - Прошлый год
                        🏆 /allPeriod - Всё время
                        🔍 /exactPeriod - Конкретные даты
                        
                        Мы покажем ваши просмотренные фильмы за выбранный период \uD83D\uDFE3.""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupShowPeriod);
                stateService.setState(chatId, UserState.WAITING_SHOW_TYPE);
                break;
            case "/threeMonths", "/lastMonth", "/lastYear":
                watchedListService.showWatchedFilmsListByPeriod(chatId, command).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupShowPeriod);
                                    stateService.setState(chatId, UserState.WAITING_SHOW_TYPE);
                                }
                        );
                break;
            case "/allPeriod" :
                watchedListService.showWatchedFilmsListByAllPeriod(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupShowPeriod);
                                    stateService.setState(chatId, UserState.WAITING_SHOW_TYPE);
                                }
                        );
                break;
            case "/exactPeriod":
                sendResponse(chatId, """
                        📆 Введите период в формате:
                        <b>дд.мм.гггг - дд.мм.гггг</b>
                        
                        Пример: <b>01.09.2024 - 30.09.2024</b>""");
                stateService.setState(chatId, UserState.WAITING_PERIOD);
                break;
            case "/searchByPref":
                searchFilmService.searchFilmByPreferences(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    List<String> resultFilms = List.of(result.split("---CUTHERESPLITTER---"));

                                    stateService.setState(chatId, UserState.WAITING_SEARCH_COMMAND);
                                    //response.append(result);
                                    resultFilms.forEach(filmMessage -> {
                                        sendResponseWithKeyboardMarkup(chatId, filmMessage, replyKeyboardMarkupSearch);
                                    });

                                }
                        );
                break;
            case "/searchRandom":
                searchFilmService.searchRandomFilm().subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupSearch);
                                    stateService.setState(chatId, UserState.WAITING_SEARCH_COMMAND);
                                }
                        );
                break;
            case "/searchByFilters":
                response.append("""
                        ⚙️ <b>Настройка фильтров поиска:</b>
                        
                        🎭 /chooseGenres - Выбрать жанры
                        🌟 /chooseActors - Выбрать актеров
                        ⭐ /chooseRate - Выбрать рейтинг
                        ⏱️ /chooseDuration - Выбрать длительность
                        📅 /chooseYears - Выбрать год выпуска
                        🌍 /chooseCountry - Выбрать страну
                        \uD83D\uDC40 /showFilters - Посмотреть выбранные фильтры
                        ✅ /applyFilters - Применить фильтры
                        
                        ⚠️ При повторном выборе параметр будет сброшен.""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_FILTER);
                break;
            case "/chooseGenres":
                response.append("🎭 Введите жанры через запятую для добавления в фильтры:");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_GENRE_FILTER);
                break;
            case "/chooseActors":
                response.append("\uD83C\uDF1F Введите имена актеров через запятую для добавления в фильтры:");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_ACTOR_FILTER);
                break;
            case "/chooseRate":
                response.append("""
                        ⭐ <b>Введите рейтинг для добавления в фильтры:</b>
                        • Диапазон: 7-9
                        • Конкретные значения: 8, 8.5, 9
                        
                        От 0 до 10 (например: 7.5-9.2)
                        """);
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_RATE_FILTER);
                break;
            case "/chooseDuration":
                response.append("""
                        ⏳ <b>Введите длительность (в минутах) для добавления в фильтры:</b>
                        Формат: 90-120
                        
                        Минимум: 0 мин
                        Максимум: 51420 мин (≈35 дней) 😅""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_DURATION_FILTER);
                break;
            case "/chooseYears":
                response.append("""
                        📅 <b>Введите годы выпуска для добавления в фильтры:</b>
                        • Диапазон: 2000-2010
                        • Конкретные годы: 2015, 2018, 2020""");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_YEAR_FILTER);
                break;
            case "/chooseCountry":
                response.append("🌎 Введите страны производства для добавления в фильтры:");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                stateService.setState(chatId, UserState.WAITING_COUNTRY_FILTER);
                break;
            case "/applyFilters":
                searchFilmService.applySearchFilters(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    List<String> resultFilms = List.of(result.split("---CUTHERESPLITTER---"));
                                    if (!resultFilms.isEmpty()) {
                                        sendResponseWithKeyboardMarkup(chatId, "✨ Вот найденные фильмы по вашим фильтрам:", replyKeyboardMarkupSearch);
                                    } else {
                                        sendResponseWithKeyboardMarkup(chatId, "К сожалению, не смог найти фильмы по вашим параметрам\uD83D\uDE22", replyKeyboardMarkupSearch);
                                    }
                                    stateService.setState(chatId, UserState.WAITING_SEARCH_COMMAND);
                                    //response.append(result);
                                    resultFilms.forEach(filmMessage -> {
                                        sendResponse(chatId, filmMessage);
                                    });

                                }
                        );
                break;
            case "/showFilters":
                searchFilmService.showFilters(chatId).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            default:
                response.append("🤷‍♂️ Ой! Такой команды нет. Чтобы узнать список доступных команд, воспользуйся:\n" +
                        "/menu - список доступных команд.");
                sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupMenu);
        }
    }


    private void handleStatefulInput(String input, Long chatId, String username) {
        StringBuilder response = new StringBuilder();
        UserState currentState = stateService.getState(chatId);
        switch (currentState) {
            case IDLE:
                handleMainCommand(input, chatId, username);
                break;
            case WAITING_FILM_TITLE_FOR_INF:
                infoAboutFilmServiceService.getInfoAboutFilm(input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(),replyKeyboardMarkupMenu);

                                    stateService.setState(chatId, UserState.IDLE);
                                }
                        );
                break;
            case WAITING_WATCH_LIST_COMMAND:
                if (input.equals("/addToWatchList") || input.equals("/deleteFromWatchList") || input.equals("/showWatchList")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверная команда</b> ⚠️
                            
                            Доступные команды:
                            ➕ /addToWatchList - Добавить фильм
                            🗑️ /deleteFromWatchList - Удалить фильм
                            📋 /showWatchList - Показать список
                            
                            Попробуйте еще раз 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchList);
                    stateService.setState(chatId,UserState.WAITING_WATCH_LIST_COMMAND);
                }
                break;
            case WAITING_FILM_TITLE_FOR_ADD_TO_WATCH_LIST:
                watchListService.addToWatchList(chatId, input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchList);
                                    stateService.setState(chatId, UserState.WAITING_WATCH_LIST_COMMAND);
                                }
                        );
                break;
            case WAITING_FILM_TITLE_FOR_DELETE_FROM_WATCH_LIST:
                watchListService.deleteFromWatchList(chatId, input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchList);
                                    stateService.setState(chatId, UserState.WAITING_WATCH_LIST_COMMAND);
                                }
                        );
                break;
            case WAITING_PREFERENCE_COMMAND:
                if (input.equals("/addPreferences") || input.equals("/deletePreferences") || input.equals("/showMyPreferences")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверная команда</b> ⚠️
                            
                            Доступные команды:
                            ➕ /addPreferences - Добавить предпочтения
                            🗑️ /deletePreferences - Удалить предпочтения
                            👀 /showMyPreferences - Показать мои предпочтения
                            
                            Попробуйте еще раз 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupPreferences);
                    stateService.setState(chatId,UserState.WAITING_PREFERENCE_COMMAND);
                }
                break;
            case WAITING_PREF_TYPE_FOR_ADD:
                if (input.equals("/addGenrePreferences") || input.equals("/addActorPreferences") || input.equals("/addCountryPreferences")||
                input.equals("/addYearPreferences")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверный ввод</b> ⚠️
                            
                            Доступные команды для добавления:
                            🎭 /addGenrePreferences - Жанры
                            🌟 /addActorPreferences - Актеры
                            🌍 /addCountryPreferences - Страны
                            📅 /addYearPreferences - Годы
                            
                            Попробуйте еще раз! 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_ADD);
                }
                break;
            case WAITING_GENRE_FOR_ADD:
                preferencesService.addPreferences(chatId, input, "genre").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_ADD);
                                }
                        );
                break;
            case WAITING_ACTOR_FOR_ADD:
                preferencesService.addPreferences(chatId, input, "actor").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_ADD);
                                }
                        );

                break;
            case WAITING_COUNTRY_FOR_ADD:
                preferencesService.addPreferences(chatId, input, "country").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_ADD);
                                }
                        );
                break;
            case WAITING_YEAR_FOR_ADD:
                preferencesService.addPreferences(chatId, input, "year").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupAddPreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_ADD);
                                }
                        );
                break;
            case WAITING_PREF_TYPE_FOR_DELETE:
                if (input.equals("/deleteGenrePreferences") || input.equals("/deleteActorPreferences") || input.equals("/deleteCountryPreferences")||
                        input.equals("/deleteYearPreferences")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверный ввод</b> ⚠️
                            
                            Доступные команды для добавления:
                            🎭 /deleteGenrePreferences - Жанры
                            👨‍🎤 /deleteActorPreferences - Актеры
                            🌎 /deleteCountryPreferences - Страны
                            📆 /deleteYearPreferences - Годы
                            
                            Попробуйте еще раз! 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_DELETE);
                }
                break;
            case WAITING_GENRE_FOR_DELETE:
                preferencesService.deletePreferences(chatId, input, "genre").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_DELETE);
                                }
                        );
                break;
            case WAITING_ACTOR_FOR_DELETE:
                preferencesService.deletePreferences(chatId, input, "actor").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_DELETE);
                                }
                        );
                break;
            case WAITING_COUNTRY_FOR_DELETE:
                preferencesService.deletePreferences(chatId, input, "country").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_DELETE);
                                }
                        );
                break;
            case WAITING_YEAR_FOR_DELETE:
                preferencesService.deletePreferences(chatId, input, "year").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupDeletePreferences);
                                    stateService.setState(chatId,UserState.WAITING_PREF_TYPE_FOR_DELETE);
                                }
                        );
                break;
            case WAITING_WATCHED_LIST_COMMAND:
                if (input.equals("/addToWatchedFilmsList") || input.equals("/addMarkToWatchedFilm") || input.equals("/addReviewToWatchedFilm")||
                        input.equals("/showWatchedFilmsList")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверный ввод</b> ⚠️
                            
                            Доступные команды для работы с просмотренными фильмами:
                            🎬 /addToWatchedFilmsList - Добавить фильм
                            ⭐ /addMarkToWatchedFilm - Поставить оценку
                            ✏️ /addReviewToWatchedFilm - Написать отзыв
                            📋 /showWatchedFilmsList - Показать список
                            
                            Попробуйте еще раз! 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchedList);
                    stateService.setState(chatId,UserState.WAITING_WATCHED_LIST_COMMAND);
                }
                break;
            case WAITING_FILM_TITLE_FOR_ADD_TO_WATCHED_LIST:
                watchedListService.addToWatchedFilmsList(chatId, input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchedList);
                                    stateService.setState(chatId,UserState.WAITING_WATCHED_LIST_COMMAND);
                                }
                        );
                break;
            case WAITING_FILM_TITLE_FOR_ADD_MARK:
                watchedListService.checkingForMovieExistence(chatId, input)
                        .subscribeOn(Schedulers.boundedElastic())
                        .switchIfEmpty(Mono.fromRunnable(() -> {
                            response.append("😕 Фильм \"").append(input).append("\" не найден. Проверьте название и повторите ввод.");
                            sendResponse(chatId, response.toString());
                            stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_MARK);
                        }))
                        .subscribe(filmId -> {
                            response.append("🎉 Отлично! Фильм \"").append(input).append("\" найден!\n\n")
                                    .append("📝 Пожалуйста, оцените его целым числом от 0 до 10:");
                            sendResponse(chatId, response.toString());
                            stateService.setState(chatId, UserState.WAITING_MARK);
                        });
                break;

            case WAITING_MARK:
                watchedListService.addMarkToWatchedFilm(chatId, input)
                        .subscribeOn(Schedulers.boundedElastic())
                        .switchIfEmpty(Mono.fromRunnable(() -> {
                            response.append("⚠️ Ой! Кажется, вы ошиблись.\n")
                                    .append("Оценка должна быть целым числом от 0 до 10.\n")
                                    .append("Попробуйте еще раз 😊");
                            sendResponse(chatId, response.toString());
                            stateService.setState(chatId, UserState.WAITING_MARK);
                        }))
                        .subscribe(result -> {
                            response.append(result);
                            sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchedList);
                            stateService.setState(chatId, UserState.WAITING_WATCHED_LIST_COMMAND);
                        });
                break;

            case WAITING_FILM_TITLE_FOR_ADD_REVIEW:
                watchedListService.checkingForMovieExistence(chatId, input)
                        .subscribeOn(Schedulers.boundedElastic())
                        .switchIfEmpty(Mono.fromRunnable(() -> {
                            response.append("😕 Фильм \"").append(input).append("\" не найден. Проверьте название и повторите ввод.");
                            sendResponse(chatId, response.toString());
                            stateService.setState(chatId, UserState.WAITING_FILM_TITLE_FOR_ADD_REVIEW);
                        }))
                        .subscribe(filmId -> {
                            response.append("✅ Фильм \"").append(input).append("\" найден!\n\n")
                                    .append("✏️ Напишите ваш отзыв в следующем сообщении.\n")
                                    .append("Можно поделиться впечатлениями, оценкой актерской игры или сюжета.");
                            sendResponse(chatId, response.toString());
                            stateService.setState(chatId, UserState.WAITING_REVIEW);
                        });
                break;
            case WAITING_REVIEW:
                watchedListService.addReviewToWatchedFilm(chatId, input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupWatchedList);
                                    stateService.setState(chatId, UserState.WAITING_WATCHED_LIST_COMMAND);
                                }
                        );
                break;
            case WAITING_SHOW_TYPE:
                    if (input.equals("/threeMonths") || input.equals("/lastMonth") || input.equals("/lastYear")||
                    input.equals("/allPeriod")|| input.equals("/exactPeriod")) {
                        handleCommand(input, chatId);
                    } else {
                        response.append("⚠️ <b>Неверная команда</b> ⚠️\n\n")
                                .append("Доступные периоды:\n")
                                .append("⏳ /threeMonths - 3 месяца\n")
                                .append("📅 /lastMonth - Последний месяц\n")
                                .append("🎉 /lastYear - Прошлый год\n")
                                .append("🏆 /allPeriod - Все время\n")
                                .append("🔍 /exactPeriod - Конкретные даты\n")
                                .append("Попробуйте еще раз!");
                        sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupShowPeriod);
                        stateService.setState(chatId, UserState.WAITING_SHOW_TYPE);
                    }
                break;
            case WAITING_PERIOD:
                watchedListService.showWatchedFilmsListByExactPeriod(chatId, input).subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupShowPeriod);
                                    stateService.setState(chatId, UserState.WAITING_SHOW_TYPE);
                                }
                        );
                break;
            case WAITING_SEARCH_COMMAND:
                if (input.equals("/searchByPref") || input.equals("/searchRandom") || input.equals("/searchByFilters")){
                    handleCommand(input, chatId);
                }
                else {
                    response.append("""
                            ⚠️ <b>Неверный ввод</b> ⚠️
                            
                            Доступные команды для работы с фильтрами:
                            🎛️ A. /searchByFilters - Поиск по фильтрам
                            ❤️ B. /searchByPref - Поиск по вашим предпочтениям
                            🎲 C. /searchRandom - Случайная рекомендация
                            
                            Попробуйте еще раз! 😊""");
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupSearch);
                    stateService.setState(chatId,UserState.WAITING_SEARCH_COMMAND);

                }
                break;
            case WAITING_FILTER:
                if (input.equals("/chooseGenres") || input.equals("/chooseActors") || input.equals("/chooseRate")||
                        input.equals("/chooseDuration")||input.equals("/chooseYears")||input.equals("/chooseCountry")
                || input.equals("/applyFilters") || input.equals("/showFilters")){
                    handleCommand(input, chatId);
                }
                else{
                    response.append("""
                            ⚠️ <b>Неверный ввод</b> ⚠️
                            
                            Доступные команды для работы с настройки фильтров:
                            🎭 /chooseGenres - Выбрать жанры
                            🌟 /chooseActors - Выбрать актеров
                            ⭐ /chooseRate - Выбрать рейтинг
                            ⏱️ /chooseDuration - Выбрать длительность
                            📅 /chooseYears - Выбрать год выпуска
                            🌍 /chooseCountry - Выбрать страну
                            \uD83D\uDC40 /showFilters - Посмотреть выбранные фильтры
                            ✅ /applyFilters - Применить фильтры
                            
                            ⚠️ При повторном выборе параметр будет сброшен
                            """);
                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                    stateService.setState(chatId,UserState.WAITING_FILTER);
                }
                break;
            case WAITING_ACTOR_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "actor").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            case WAITING_GENRE_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "genre").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            case WAITING_RATE_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "rate").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            case WAITING_DURATION_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "duration").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            case WAITING_YEAR_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "year").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            case WAITING_COUNTRY_FILTER:
                searchFilmService.addSearchFilter(chatId, input, "country").subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                result -> {
                                    response.append(result);
                                    sendResponseWithKeyboardMarkup(chatId, response.toString(), replyKeyboardMarkupFilters);
                                    stateService.setState(chatId, UserState.WAITING_FILTER);
                                }
                        );
                break;
            default:
                sendResponse(chatId, "Неизвестное состояние.");
        }
    }

    private void sendResponse(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.setParseMode("HTML");
    try {
        execute(message);
    } catch (TelegramApiException e) {
        System.out.println("Ошибка при отправке сообщения: " + e);}
    }

    private void sendResponseWithKeyboardMarkup(Long chatId, String text, Mono<ReplyKeyboardMarkup> keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML");
        keyboardMarkup.subscribeOn(Schedulers.boundedElastic()).subscribe(
                message::setReplyMarkup
        );
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения с клавиатурой: " + e);}
    }
}