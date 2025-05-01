package ru.spbstu.movierecbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@PropertySource("classpath:bot.properties")
public class WebClientConfig {

    @Value("${api.key}")
    private String key;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient kinopoiskWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api.kinopoisk.dev/v1.4")
                .defaultHeader("X-API-KEY", key)
                .build();
    }
}