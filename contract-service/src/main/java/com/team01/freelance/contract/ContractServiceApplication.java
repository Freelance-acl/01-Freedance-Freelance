package com.team01.freelance.contract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.contract"
})
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
public class ContractServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractServiceApplication.class, args);
    }

}
