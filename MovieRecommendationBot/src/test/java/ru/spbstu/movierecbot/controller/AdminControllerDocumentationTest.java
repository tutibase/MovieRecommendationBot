package ru.spbstu.movierecbot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import ru.spbstu.movierecbot.config.TestMockConfig;
import ru.spbstu.movierecbot.config.WebConfig;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@ContextConfiguration(classes = {
        WebConfig.class,
        TestMockConfig.class
})
@EnableWebFlux
public class AdminControllerDocumentationTest {

    @Autowired
    private AdminController adminController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.webTestClient = WebTestClient.bindToController(adminController)
                .configureClient()
                .filter(WebTestClientRestDocumentation.documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(Preprocessors.prettyPrint())
                        .withResponseDefaults(Preprocessors.prettyPrint()))
                .build();
    }

    @Test
    void getUsersExample() {
        webTestClient.get()
                .uri("/admin/users?password=admin")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("get-users",
                        RequestDocumentation.queryParameters(
                                RequestDocumentation.parameterWithName("password").description("Пароль администратора")
                        ),
                        PayloadDocumentation.responseFields(
                                PayloadDocumentation.fieldWithPath("[].telegramId").description("ID пользователя в Telegram"),
                                PayloadDocumentation.fieldWithPath("[].createdAt").description("Дата создания аккаунта")
                        )));
    }
}