package ru.spbstu.movierecbot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.spbstu.movierecbot.dao.filmapi.FilmDao;
import ru.spbstu.movierecbot.dto.FilmDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InfoAboutFilmService {

    private final FilmDao filmDao;

    @Autowired
    public InfoAboutFilmService(FilmDao filmDao) {
        this.filmDao = filmDao;
    }

    public Mono<String> getInfoAboutFilm(String filmTitle) {
        return filmDao.getFilmByName(filmTitle)
                .flatMap(filmDto -> {
                    String formattedInfo = formatFilmDetails(filmDto);
                    return Mono.just(formattedInfo);
                })
                .onErrorResume(error -> Mono.just("Ошибка при поиске фильма: " + error.getMessage()));
    }

    private String formatFilmDetails(FilmDto film) {
        return String.format(
                """
                <b>🎥 %s (%d)</b> %s
        
                <b>🌟 Рейтинги:</b>
                🎞 <b>Кинопоиск:</b> %s
                🌐 <b>IMDb:</b> %s
        
                <b>📜 Описание:</b>
                🔞 <b>Возраст:</b> %s
                🏷 <b>Жанры:</b> %s
                🌍 <b>Страны:</b> %s
        
                <b>👥 Актеры:</b>
                %s
        
                <b>💰 Финансы:</b>
                ⏱ <b>Длительность:</b> %d мин.
                💵 <b>Бюджет:</b> %s
                🏦 <b>Сборы в мире:</b> %s
        
                <b>🎬 Похожие фильмы:</b>
                %s
                """,
                escapeHtml(film.russianTitle()),
                film.premiereYear(),
                getRatingEmoji(film.rating().kinopoiskRating()),
                escapeHtml(formatRating(film.rating().kinopoiskRating())),
                escapeHtml(formatRating(film.rating().imdbRating())),
                escapeHtml(film.ageLimit()),
                escapeHtml(formatGenres(film.genres())),
                escapeHtml(formatCountries(film.countries())),
                escapeHtml(formatActors(film.personsData())),
                film.duration(),
                escapeHtml(formatBudget(film.budget())),
                escapeHtml(formatFees(film.fees())),
                escapeHtml(formatSimilarFilms(film.similarFilmsData()))
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // Вспомогательные методы форматирования
    private String formatRating(Double rating) {
        return rating != null ? String.format("%.1f", rating) : "—";
    }

    private String formatGenres(List<FilmDto.Genre> genres) {
        return genres.stream()
                .map(FilmDto.Genre::name)
                .limit(5)
                .collect(Collectors.joining(", "));
    }

    private String formatCountries(List<FilmDto.Country> countries) {
        return countries.stream()
                .map(FilmDto.Country::name)
                .limit(3)
                .collect(Collectors.joining(", "));
    }

    private String formatActors(List<FilmDto.Person> persons) {
        if (persons == null || persons.isEmpty()) return "—";
        return persons.stream()
                .limit(5)
                .map(f -> "• " + f.name())
                .collect(Collectors.joining("\n"));
    }

    private String formatBudget(FilmDto.Budget budget) {
        if (budget == null) return "—";
        return String.format("%,d", budget.value());
    }

    private String formatFees(FilmDto.Fees fees) {
        if (fees == null || fees.worldFee() == null) return "—";
        return String.format("%,d", fees.worldFee().value());
    }

    private String formatSimilarFilms(List<FilmDto.SimilarMovie> films) {
        if (films == null || films.isEmpty()) return "—";
        return films.stream()
                .limit(3)
                .map(f -> "• " + f.name())
                .collect(Collectors.joining("\n"));
    }

    private String getRatingEmoji(Double kpRating) {
        if (kpRating == null) return "";
        if (kpRating >= 8.0) return "🔥";
        if (kpRating >= 6.0) return "👍";
        return "";
    }
}
