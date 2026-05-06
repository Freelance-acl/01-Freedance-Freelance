package com.team01.freelance.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.wallet"
})
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
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }

}
