package ru.spbstu.movierecbot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import ru.spbstu.movierecbot.config.TestMockConfig;
import ru.spbstu.movierecbot.config.WebConfig;

import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@ContextConfiguration(classes = {
        WebConfig.class,
        TestMockConfig.class
})
@EnableWebFlux
public class HealthCheckControllerDocumentationTest {

    @Autowired
    private HealthCheckController healthCheckController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = WebTestClient.bindToController(healthCheckController)
                .configureClient()
                .filter(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void healthCheckExample() {
        webTestClient.get()
                .uri("/healthcheck")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(document("healthcheck",
                        responseBody()
                ));
    }
}