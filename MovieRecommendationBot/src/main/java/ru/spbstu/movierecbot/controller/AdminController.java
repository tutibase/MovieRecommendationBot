package ru.spbstu.movierecbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dto.UserDto;
import ru.spbstu.movierecbot.services.AdminService;


@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Flux<UserDto> getUsers(@RequestParam("password") String password) {
        return adminService.getUsersIfAdmin(password)
                .map(usersRecord -> new UserDto(
                        usersRecord.getTelegramId(),
                        usersRecord.getCreatedAt()
                ));
    }
}