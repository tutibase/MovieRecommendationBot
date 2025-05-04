package ru.spbstu.movierecbot.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.controller.AdminController;
import ru.spbstu.movierecbot.controller.HealthCheckController;
import ru.spbstu.movierecbot.dbClasses.tables.records.UsersRecord;
import ru.spbstu.movierecbot.services.AdminService;

import java.time.LocalDate;

@Configuration
@EnableWebFlux
@Import(WebConfig.class)
public class TestMockConfig {

    @Bean
    @Primary  // Переопределяем основной бин
    public AdminService adminService() {
        AdminService mockService = Mockito.mock(AdminService.class);

        // Настраиваем mock
        Mockito.when(mockService.getUsersIfAdmin(Mockito.anyString()))
                .thenReturn(Flux.just(
                        new UsersRecord(1234L, LocalDate.now()),
                        new UsersRecord(5678L, LocalDate.now().minusDays(1))
                ));

        return mockService;
    }

    @Bean
    public AdminController adminController() {
        return new AdminController(adminService());
    }

    @Bean
    public HealthCheckController healthCheckController() {
        return new HealthCheckController();
    }


}