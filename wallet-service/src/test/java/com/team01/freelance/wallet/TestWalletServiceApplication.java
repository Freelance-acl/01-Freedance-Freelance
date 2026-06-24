package com.team01.freelance.wallet;

import com.team01.freelance.user.config.JwtConfig;
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
 * production WalletServiceApplication to prevent duplicate Feign client registration.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.team01.freelance.wallet"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WalletServiceApplication.class
        )
)
@EnableCaching
@EnableFeignClients
@Import({JwtConfig.class})
@EnableJpaRepositories(basePackages = {
        "com.team01.freelance.wallet",
        "com.team01.freelance.contract",
        "com.team01.freelance.user"
})
@EntityScan(basePackages = {
        "com.team01.freelance.wallet",
        "com.team01.freelance.contract",
        "com.team01.freelance.user"
})
public class TestWalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestWalletServiceApplication.class, args);
    }
}
