package com.team01.freelance.wallet.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestApplicationConfig {

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String dotPath) {
        String[] keys = dotPath.split("\\.");
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(key);
        }
        return current;
    }

    private Map<String, Object> loadWalletYamlConfig() throws Exception {
        File baseDir = new File("src/main/resources");
        File ymlFile = new File(baseDir, "application.yml");
        File forbiddenPropsFile = new File(baseDir, "application.properties");

        assertFalse(forbiddenPropsFile.exists(), "wallet-service should not contain an application.properties file!");
        assertTrue(ymlFile.exists(), "wallet-service is missing its application.yml configuration file!");

        try (InputStream input = new FileInputStream(ymlFile)) {
            return new Yaml().load(input);
        }
    }

    @Test
    public void verifyWalletServiceMilestone2ConfigSpecs() throws Exception {
        Map<String, Object> config = loadWalletYamlConfig();

        String dbUrl = (String) getNestedValue(config, "spring.datasource.url");
        assertNotNull(dbUrl, "wallet-service is missing spring.datasource.url");
        assertTrue(dbUrl.contains("postgres:5432"), "wallet-service datasource URL must point to postgres:5432");

        assertNotNull(getNestedValue(config, "spring.data.mongodb.uri"), "wallet-service is missing spring.data.mongodb.uri");
        assertNotNull(getNestedValue(config, "spring.data.redis.host"), "wallet-service is missing spring.data.redis.host");
        assertNotNull(getNestedValue(config, "jwt.secret"), "wallet-service is missing jwt.secret");
        assertNotNull(getNestedValue(config, "jwt.expiration"), "wallet-service is missing jwt.expiration");
    }
}