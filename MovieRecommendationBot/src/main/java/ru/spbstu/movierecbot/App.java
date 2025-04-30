package ru.spbstu.movierecbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.http.server.HttpServer;
import ru.spbstu.movierecbot.config.AppConfig;

import ru.spbstu.movierecbot.dao.UserDao;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;

import java.util.Optional;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Создаем роутер
        RouterFunction<ServerResponse> router = route(
                GET("/healthcheck"),
                request -> ServerResponse.ok().bodyValue("Server is running!\n1. Bogdanova\n2. Lugovenko\n3. Yakunin")
        );

        // Конвертируем роутер в HttpHandler
        HttpHandler httpHandler = RouterFunctions.toHttpHandler(router);

        logger.info("Server started on http://localhost:8110");

        // Получаем бин UserDao из контекста
        UserDao userDao = context.getBean(UserDao.class);
        userDao.createUser(12345678);
        logger.info("Пользователь создан.");

        // Тестируем получение пользователя
        Optional<UsersRecord> user = userDao.getUserByTelegramId(12345678);
        if (user.isPresent()) {
            logger.info("Пользователь найден: {}", user.get().getTelegramId());
        } else {
            logger.warn("Пользователь не найден.");
        }

        // Запускаем сервер
        HttpServer.create()
                .host("localhost")
                .port(8110)
                .handle(new ReactorHttpHandlerAdapter(httpHandler))
                .bindNow()
                .onDispose()
                .block();


    }
}