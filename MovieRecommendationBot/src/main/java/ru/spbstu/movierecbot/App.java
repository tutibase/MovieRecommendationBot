package ru.spbstu.movierecbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.netty.http.server.HttpServer;
import ru.spbstu.movierecbot.config.AppConfig;


public class App {
    @Value("${http.port}") static Integer httpPort;
    @Value("${http.host}") static String httpHost;
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Автоконфигурация через @EnableWebFlux
        HttpHandler httpHandler = WebHttpHandlerBuilder
                .applicationContext(context)
                .build();

        // Запускаем сервер
        HttpServer.create()
                .host(httpHost)
                .port(httpPort)
                .handle(new ReactorHttpHandlerAdapter(httpHandler))
                .bindNow()
                .onDispose()
                .block();

    }
}