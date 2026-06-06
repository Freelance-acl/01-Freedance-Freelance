package com.team01.freelance.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.job"
})
@EnableCaching
@EnableJpaRepositories(basePackages = {
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
@EntityScan(basePackages = {
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }

}
