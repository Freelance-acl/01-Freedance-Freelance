package com.team01.freelance.contract.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractDashboardTest {

    @Test
    void contractDashboardContainsAtLeastThreeLogQlPanels() throws Exception {
        JsonNode dashboard = new ObjectMapper().readTree(Files.readString(contractDashboardPath()));

        long logQlPanels = StreamSupport.stream(dashboard.path("panels").spliterator(), false)
                .filter(panel -> "logs".equals(panel.path("type").asText()))
                .filter(this::usesLokiDatasource)
                .filter(panel -> StreamSupport.stream(panel.path("targets").spliterator(), false)
                        .map(target -> target.path("expr").asText(""))
                        .anyMatch(expr -> !expr.isBlank()))
                .count();

        assertTrue(logQlPanels >= 3, "contract-dashboard.json must include at least three LogQL panels");
    }

    private boolean usesLokiDatasource(JsonNode panel) {
        JsonNode datasource = panel.path("targets").path(0).path("datasource");
        if (datasource.isTextual()) {
            return "Loki".equals(datasource.asText());
        }
        return "Loki".equals(datasource.path("type").asText())
                || "Loki".equals(datasource.path("uid").asText());
    }

    private Path contractDashboardPath() {
        Path fromRoot = Path.of("k8s/monitoring/grafana/dashboards/contract-dashboard.json");
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Path.of("../k8s/monitoring/grafana/dashboards/contract-dashboard.json");
    }
}
