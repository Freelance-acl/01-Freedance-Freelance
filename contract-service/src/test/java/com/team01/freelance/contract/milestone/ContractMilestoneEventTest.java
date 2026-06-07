package com.team01.freelance.contract.milestone;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContractMilestoneEventTest {

    @Test
    void cassandraKeyAnnotationsMatchMilestoneTimelineShape() throws Exception {
        Field contractId = ContractMilestoneEvent.class.getDeclaredField("contractId");
        Field timestamp = ContractMilestoneEvent.class.getDeclaredField("timestamp");

        assertNotNull(contractId.getAnnotation(PartitionKey.class));
        assertNotNull(timestamp.getAnnotation(ClusteringColumn.class));
        assertEquals(0, timestamp.getAnnotation(ClusteringColumn.class).value());
    }
}
