package ru.spbstu.movierecbot.services.state;

public enum UserState {
    // Состояния главного меню
    IDLE, // Пользователь не в процессе команды
    WAITING_SEARCH_COMMAND, // Ожидание команды для поиска фильма (/searchFilm)
    WAITING_WATCH_LIST_COMMAND, // Ожидание команды для работы со списком "Буду смотреть"(/watchlist)
    WAITING_PREFERENCE_COMMAND, // Ожидание команды для работы с предпочтениями(/preferences)
    WAITING_WATCHED_LIST_COMMAND, // Ожидание команды для работы со списком просмотренных фильмов(/watchedList)
    WAITING_FILM_TITLE_FOR_INF, // Ожидание названия фильма для вывода информации о фильме(/infoAboutFilm)

    // Состояния после WAITING_SEARCH_COMMAND
    WAITING_FILTER, //Ожидание выбора типа фильтра(/searchByFilters)
    WAITING_ACTOR_FILTER, // Ожидание ввода актера(/chooseActors)
    WAITING_YEAR_FILTER, // Ожидание ввода года(/chooseYear)
    WAITING_COUNTRY_FILTER, // Ожидание ввода страны(/chooseCountry)
    WAITING_GENRE_FILTER, // Ожидание ввода жанра(/chooseGenres)
    WAITING_RATE_FILTER, // Ожидание ввода рейтинга(/chooseRate)
    WAITING_DURATION_FILTER, // Ожидание ввода длительности(/chooseDuration)

    //Состояния после WAITING_WATCH_LIST_COMMAND
    WAITING_FILM_TITLE_FOR_ADD_TO_WATCH_LIST, // Ожидание ввода названия фильма для добавления в список "Буду смотреть"(/addToWatchList)
    WAITING_FILM_TITLE_FOR_DELETE_FROM_WATCH_LIST, // Ожидание ввода названия фильма для удаления из списка "Буду смотреть"(/deleteFromWatchList)

    //Состояния после WAITING_WATCHED_LIST_COMMAND
    WAITING_SHOW_TYPE, // Ожидание выбора периода для вывода списка просмотренных(/showWatchedFilmsList)
    WAITING_FILM_TITLE_FOR_ADD_TO_WATCHED_LIST, // Ожидание названия фильма для добавления в список просмотренных(/addToWatchedFilmsList)
    WAITING_FILM_TITLE_FOR_ADD_MARK, // Ожидание названия фильма для добавления оценки(/addMarkToWatchedFilm)
    WAITING_FILM_TITLE_FOR_ADD_REVIEW, // Ожидания названия фильма для добавления отзыва (/addReviewToWatchedFilm)
    WAITING_PERIOD, // Ожидание периода (/exactPeriod)
    WAITING_MARK, // Ожидание оценки
    WAITING_REVIEW, //Ожидание отзыва

    // Состояния после WAITING_PREFERENCE_COMMAND
    WAITING_PREF_TYPE_FOR_DELETE, // Ожидание типа предпочтений для удаления
    WAITING_PREF_TYPE_FOR_ADD, // Ожидание типа предпочтений для добавления

    // Состояния после WAITING_PREF_TYPE_FOR_DELETE
    WAITING_GENRE_FOR_DELETE, // Ожидание ввода жанра
    WAITING_ACTOR_FOR_DELETE, // Ожидание ввода актера
    WAITING_COUNTRY_FOR_DELETE, // Ожидание ввода страны
    WAITING_YEAR_FOR_DELETE, // Ожидание ввода года

    // Состояния после WAITING_PREF_TYPE_FOR_ADD
    WAITING_GENRE_FOR_ADD, // Ожидание ввода жанра
    WAITING_ACTOR_FOR_ADD, // Ожидание ввода актера
    WAITING_COUNTRY_FOR_ADD, // Ожидание ввода страны
    WAITING_YEAR_FOR_ADD, // Ожидание ввода года

}
