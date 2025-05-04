package ru.spbstu.movierecbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public Jackson2JsonEncoder jsonEncoder() {
        return new Jackson2JsonEncoder(objectMapper());
    }

    @Bean
    public Jackson2JsonDecoder jsonDecoder() {
        return new Jackson2JsonDecoder(objectMapper());
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(jsonEncoder());
        configurer.defaultCodecs().jackson2JsonDecoder(jsonDecoder());
    }
}