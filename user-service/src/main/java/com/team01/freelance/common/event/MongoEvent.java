package com.team01.freelance.common.event;

import java.time.LocalDateTime;
import java.util.Map;

/** Common contract for all MongoDB audit/event documents (DP-6 / §7.1.1). */
public interface MongoEvent {

    String getId();

    LocalDateTime getTimestamp();

    String getAction();

    Map<String, Object> getDetails();
}

