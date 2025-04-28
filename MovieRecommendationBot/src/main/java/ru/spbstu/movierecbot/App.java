package ru.spbstu.movierecbot;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.server.HttpServer;
import ru.spbstu.movierecbot.config.AppConfig;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class App {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Создаем роутер
        RouterFunction<ServerResponse> router = route(
                GET("/healthcheck"),
                request -> ServerResponse.ok().bodyValue("Server is running!\n1. Bogdanova\n2. Lugovenko\n3. Yakunin")
        );

        // Конвертируем роутер в HttpHandler
        HttpHandler httpHandler = RouterFunctions.toHttpHandler(router);

        // Запускаем сервер
        HttpServer.create()
                .host("localhost")
                .port(8110)
                .handle(new ReactorHttpHandlerAdapter(httpHandler))
                .bindNow()
                .onDispose()
                .block();

        System.out.println("Server started on http://localhost:8110");
    }
}