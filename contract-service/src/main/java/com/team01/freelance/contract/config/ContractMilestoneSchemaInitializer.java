package com.team01.freelance.contract.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Arrays;

@Component
@ConditionalOnProperty(
        value = "contract.cassandra.schema-init.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ContractMilestoneSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ContractMilestoneSchemaInitializer.class);

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspaceName;

    @PostConstruct
    void initializeSchema() {
        try {
            CqlSessionBuilder builder = CqlSession.builder()
                    .withLocalDatacenter(localDatacenter);

            Arrays.stream(contactPoints.split(","))
                    .map(String::trim)
                    .filter(host -> !host.isBlank())
                    .forEach(host -> builder.addContactPoint(new InetSocketAddress(host, port)));

            try (CqlSession session = builder.build()) {
                session.execute("""
                        CREATE KEYSPACE IF NOT EXISTS %s
                        WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
                        """.formatted(keyspaceName));
                session.execute("""
                        CREATE TABLE IF NOT EXISTS %s.contract_milestone_events (
                            contract_id bigint,
                            timestamp timestamp,
                            milestone_order int,
                            status text,
                            recorded_by text,
                            notes text,
                            PRIMARY KEY ((contract_id), timestamp)
                        ) WITH CLUSTERING ORDER BY (timestamp DESC)
                        """.formatted(keyspaceName));
            }
        } catch (Exception e) {
            log.warn("Cassandra schema initialization skipped because Cassandra is unavailable", e);
        }
    }
}
