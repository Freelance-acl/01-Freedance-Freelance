package com.team01.freelance.user.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AuthEventDTO(
        Long userId,
        String action,
        LocalDateTime timestamp,
        Map<String, Object> details
) {}