package com.team01.freelance.wallet.adapter;

import com.team01.freelance.wallet.event.PayoutAuditEvent;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MongoDocumentAdapterTest {

    private final MongoDocumentAdapter adapter = new MongoDocumentAdapter();

    @Test
    void testAdapt_HandlesNullDocument() {
        assertNull(adapter.adapt(null), "Adapter should return null if input document is null");
    }

    @Test
    void testAdapt_HandlesEmptyDocument() {
        Document emptyDoc = new Document();
        PayoutAuditEvent result = adapter.adapt(emptyDoc);

        assertNotNull(result, "Adapter should return an object even for empty documents");
        assertNull(result.getPayoutId(), "PayoutId should be null if not in document");
        assertNull(result.getAction(), "Action should be null if not in document");
    }
}