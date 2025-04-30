package ru.spbstu.movierecbot.config;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.spbstu.movierecbot.controller.MovieRecBot;

@Configuration
@PropertySource("classpath:bot.properties")
@EnableWebFlux
@ComponentScan("ru.spbstu.movierecbot")
public class AppConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(MovieRecBot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
        return botsApi;
    }

}