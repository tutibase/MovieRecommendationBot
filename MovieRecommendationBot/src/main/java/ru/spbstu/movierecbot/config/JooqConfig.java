package ru.spbstu.movierecbot.config;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(@Autowired DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
