package com.team01.freelance.contract;

import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.service.JwtService;
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
 * production ContractServiceApplication to prevent duplicate Feign client registration.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.team01.freelance.contract"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ContractServiceApplication.class
        )
)
@EnableCaching
@EnableFeignClients
@Import({JwtConfig.class, JwtService.class})
@EnableJpaRepositories(basePackages = {
        "com.team01.freelance.contract",
        "com.team01.freelance.user",
        "com.team01.freelance.job"
})
@EntityScan(basePackages = {
        "com.team01.freelance.contract",
        "com.team01.freelance.user",
        "com.team01.freelance.job"
})
public class TestContractServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestContractServiceApplication.class, args);
    }
}
