package com.team01.freelance.job.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record JobEventDTO(String action, Long jobId, LocalDateTime timestamp, Map<String, Object> details) {
}
