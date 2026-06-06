package com.team01.freelance.proposal.adapter;

import org.springframework.stereotype.Component;

import com.team01.freelance.proposal.dto.JobRecommendationDTO;

@Component
public class Neo4jRecordAdapter {

    public JobRecommendationDTO adapt(java.util.Map<String, Object> record) {
        Long jobId = toLong(record.get("jobId"));
        long score = toLong(record.get("score"));
        String title = record.get("title") != null ? record.get("title").toString() : null;
        String category = record.get("category") != null ? record.get("category").toString() : null;
        return new JobRecommendationDTO(jobId, title, category, score);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
