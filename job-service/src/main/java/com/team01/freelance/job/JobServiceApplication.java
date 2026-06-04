package com.team01.freelance.job;

import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.job"
})
@EnableCaching
@EnableJpaRepositories(basePackageClasses = {
        JobRepository.class,
        UserRepository.class
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
