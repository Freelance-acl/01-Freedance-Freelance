package com.team01.freelance.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for the job service, including Elasticsearch search and Redis caching.
 */
@SpringBootApplication(scanBasePackages = {
        "com.team01.freelance.job"
})
@EnableCaching
@EnableRabbit
@EnableFeignClients
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
