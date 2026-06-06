package com.team01.freelance.job.config;

import com.team01.freelance.job.service.JobAttachmentService;
import com.team01.freelance.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobCacheAnnotationTest {

    @Test
    void jobReadMethodsUseExpectedCaches() throws Exception {
        assertCache(JobService.class, "getJobById", "job-by-id", Long.class);
        assertCache(JobService.class, "searchJobsByStatusAndBudgetRange", "S2-F1",
                String.class, Double.class, Double.class, Pageable.class);
        assertCache(JobService.class, "getJobProposalSummary", "S2-F3",
                Long.class, LocalDate.class, LocalDate.class);
        assertCache(JobService.class, "searchByRequirements", "S2-F5",
                String.class, String.class, String.class);
        assertCache(JobService.class, "getTopBudgetJobs", "S2-F6", int.class);
    }

    @Test
    void jobAttachmentLookupUsesExpectedCache() throws Exception {
        assertCache(JobAttachmentService.class, "getJobAttachmentById", "job-attachment-by-id", Long.class);
    }

    private void assertCache(Class<?> type, String methodName, String cacheName, Class<?>... parameterTypes)
            throws Exception {
        Cacheable cacheable = type.getMethod(methodName, parameterTypes).getAnnotation(Cacheable.class);
        assertNotNull(cacheable);
        assertArrayEquals(new String[]{cacheName}, cacheable.value());
    }
}
