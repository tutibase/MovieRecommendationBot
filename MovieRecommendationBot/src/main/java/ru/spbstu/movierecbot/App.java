package ru.spbstu.movierecbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.netty.http.server.HttpServer;
import ru.spbstu.movierecbot.config.AppConfig;

@Component
public class App {
    private final Integer httpPort;
    private final String httpHost;

    public App(
            @Value("${http.port}") Integer httpPort,
            @Value("${http.host}") String httpHost
    ) {
        this.httpPort = httpPort;
        this.httpHost = httpHost;
    }

    public void startServer(AnnotationConfigApplicationContext context) {
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

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        App app = context.getBean(App.class);

        app.startServer(context);
    }

}