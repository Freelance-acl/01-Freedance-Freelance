package com.team01.freelance.job;

import com.team01.freelance.job.repository.JobRepository;
import com.team01.freelance.user.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the job service, including Elasticsearch search and Redis caching.
 */
@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.job"
})
@EnableCaching
@EnableFeignClients
@EnableJpaRepositories(basePackageClasses = {
        JobRepository.class,
        UserRepository.class
})
@EntityScan(basePackages = {
        "com.team01.freelance.job",
        "com.team01.freelance.user"
})
public class JobServiceApplication {

    /**
     * Boots the Spring application.
     *
     * @param args standard JVM arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }

}
