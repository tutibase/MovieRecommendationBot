package ru.spbstu.movierecbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Bean
    public Jackson2JsonEncoder jsonEncoder() {
        return new Jackson2JsonEncoder();
    }

    @Bean
    public Jackson2JsonDecoder jsonDecoder() {
        return new Jackson2JsonDecoder();
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(jsonEncoder());
        configurer.defaultCodecs().jackson2JsonDecoder(jsonDecoder());
    }
}