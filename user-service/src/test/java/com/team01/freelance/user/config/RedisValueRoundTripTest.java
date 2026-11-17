package com.team01.freelance.user.config;

import com.team01.freelance.common.cache.TypedCacheMapper;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RedisValueRoundTripTest {
    @Test void cachedUserAndListRemainTypedOnSecondRead() {
        var plain = new RedisConfig().redisObjectMapper();
        var serializer = new GenericJackson2JsonRedisSerializer(TypedCacheMapper.copyOf(plain));
        var user = new User();
        user.setId(17L); user.setName("Demo"); user.setRole(UserRole.CLIENT);
        user.setStatus(UserStatus.ACTIVE); user.setCreatedAt(LocalDateTime.now());
        user.setPreferences(Map.of("bio", "Backend demo"));
        var restored = assertInstanceOf(User.class, serializer.deserialize(serializer.serialize(user)));
        assertEquals(17L, restored.getId());
        assertEquals(user.getCreatedAt(), restored.getCreatedAt());
        var users = new ArrayList<User>(); users.add(user);
        var list = assertInstanceOf(java.util.List.class, serializer.deserialize(serializer.serialize(users)));
        assertInstanceOf(User.class, list.getFirst());
        assertFalse(plain.getSerializationConfig().getDefaultTyper(null) != null,
                "The normal JSON mapper must remain unchanged");
    }
}
