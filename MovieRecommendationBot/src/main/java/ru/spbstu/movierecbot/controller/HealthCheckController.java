package ru.spbstu.movierecbot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthCheckController {

    @GetMapping("/healthcheck")
    public Mono<String> healthcheck() {
        return Mono.just("Server is running!\n1. Bogdanova\n2. Lugovenko\n3. Yakunin");
    }

}