package com.team01.freelance.proposal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.team01.freelance.proposal.repository")
public class MongoRepositoriesConfig {
}
