package com.team01.freelance.contract.milestone;

import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContractMilestoneEventTest {

    @Test
    void cassandraKeyAnnotationsMatchMilestoneTimelineShape() throws Exception {
        Field contractId = ContractMilestoneEvent.class.getDeclaredField("contractId");
        Field timestamp = ContractMilestoneEvent.class.getDeclaredField("timestamp");

        PrimaryKeyColumn contractIdKey = contractId.getAnnotation(PrimaryKeyColumn.class);
        PrimaryKeyColumn timestampKey = timestamp.getAnnotation(PrimaryKeyColumn.class);

        assertNotNull(contractIdKey);
        assertEquals(PrimaryKeyType.PARTITIONED, contractIdKey.type());

        assertNotNull(timestampKey);
        assertEquals(PrimaryKeyType.CLUSTERED, timestampKey.type());
        assertEquals(0, timestampKey.ordinal());
    }
}
