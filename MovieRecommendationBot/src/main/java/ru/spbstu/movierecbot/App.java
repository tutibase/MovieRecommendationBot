package ru.spbstu.movierecbot;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.server.HttpServer;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@ComponentScan(basePackages = "ru.spbstu.movierecbot")
public class App {
    public static void main(String[] args) throws InterruptedException {

        // Роутер для /healthcheck
        RouterFunction<ServerResponse> router = route(
                GET("/healthcheck"),
                request -> ServerResponse.ok().bodyValue("Server is running!\n1. Bogdanova\n2. Lugovenko\n3. Yakunin")
        );

        HttpHandler httpHandler = RouterFunctions.toHttpHandler(router);
        var adapter = new ReactorHttpHandlerAdapter(httpHandler);
        HttpServer.create()
                .host("localhost")
                .port(8110)
                .handle(adapter)
                .bindNow();

        System.out.println("Server started on http://localhost:8110");

        Thread.currentThread().join(); // будет работать до ручной остановки
    }
}