package com.team01.freelance.job;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.config.JwtConfigurationBootstrap;
import com.team01.freelance.user.service.JwtService;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-only entry point — imports user-service JWT beans explicitly and excludes the
 * production JobServiceApplication to prevent duplicate Feign client registration.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.team01.freelance.job"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JobServiceApplication.class
        )
)
@EnableCaching
@EnableRabbit
@EnableFeignClients
@Import({JwtConfig.class, JwtConfigurationBootstrap.class, JwtService.class})
@EnableJpaRepositories(basePackages = {
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
@EntityScan(basePackages = {
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
public class TestJobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestJobServiceApplication.class, args);
    }
}
