package com.team01.freelance.job.config;

import com.team01.freelance.job.model.Job;
import com.team01.freelance.job.model.JobAttachment;
import com.team01.freelance.job.model.JobAttachmentVerificationRequest;
import com.team01.freelance.job.model.JobRatingRequest;
import com.team01.freelance.job.service.JobAttachmentService;
import com.team01.freelance.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JobCacheInvalidationAnnotationTest {

    @Test
    void jobWritesEvictExpectedReadCaches() throws Exception {
        assertEvicts(JobService.class, "createJob", new Class<?>[]{Job.class}, "S2-F1", "S2-F5", "S2-F6");
        assertEvicts(JobService.class, "updateJob", new Class<?>[]{Long.class, Job.class},
                "job-by-id", "S2-F1", "S2-F3", "S2-F5", "S2-F6");
        assertEvicts(JobService.class, "updateJobRequirements", new Class<?>[]{Long.class, Map.class},
                "job-by-id", "S2-F5");
        assertEvicts(JobService.class, "deleteJobById", new Class<?>[]{Long.class},
                "job-by-id", "S2-F1", "S2-F3", "S2-F5", "S2-F6");
        assertEvicts(JobService.class, "deleteAllJobs", new Class<?>[]{},
                "job-by-id", "S2-F1", "S2-F3", "S2-F5", "S2-F6");
        assertEvicts(JobService.class, "rateJob", new Class<?>[]{Long.class, JobRatingRequest.class},
                "job-by-id", "S2-F1", "S2-F6");
        assertEvicts(JobService.class, "verifyJobAttachment",
                new Class<?>[]{Long.class, Long.class, JobAttachmentVerificationRequest.class},
                "job-by-id", "job-attachment-by-id");
        assertEvicts(JobService.class, "closeJob", new Class<?>[]{Long.class},
                "job-by-id", "S2-F1", "S2-F5", "S2-F6");
    }

    @Test
    void jobAttachmentWritesEvictExpectedReadCaches() throws Exception {
        assertEvicts(JobAttachmentService.class, "createJobAttachment", new Class<?>[]{JobAttachment.class},
                "job-attachment-by-id", "job-by-id");
        assertEvicts(JobAttachmentService.class, "updateJobAttachment", new Class<?>[]{Long.class, JobAttachment.class},
                "job-attachment-by-id", "job-by-id");
        assertEvicts(JobAttachmentService.class, "deleteJobAttachmentById", new Class<?>[]{Long.class},
                "job-attachment-by-id", "job-by-id");
        assertEvicts(JobAttachmentService.class, "deleteAllJobAttachments", new Class<?>[]{},
                "job-attachment-by-id", "job-by-id");
    }

    private void assertEvicts(Class<?> type, String methodName, Class<?>[] parameterTypes, String... expectedCaches)
            throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        Set<String> actualCaches = Arrays.stream(method.getAnnotation(Caching.class).evict())
                .map(CacheEvict::value)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        for (String expectedCache : expectedCaches) {
            assertTrue(actualCaches.contains(expectedCache),
                    methodName + " should evict " + expectedCache + " but evicts " + actualCaches);
        }
    }
}
