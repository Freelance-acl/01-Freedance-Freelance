package com.team01.freelance.proposal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.proposal"
})
@EnableJpaRepositories(basePackages = {
        "com.team01.freelance.proposal",
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
@EntityScan(basePackages = {
        "com.team01.freelance.proposal",
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
public class ProposalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProposalServiceApplication.class, args);
    }

}
