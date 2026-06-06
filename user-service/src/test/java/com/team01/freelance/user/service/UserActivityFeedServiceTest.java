package com.team01.freelance.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team01.freelance.user.adapter.MongoDocumentAdapter;
import com.team01.freelance.user.dto.UserActivityEventDTO;
import com.team01.freelance.user.dto.UserActivityFeedDTO;
import com.team01.freelance.user.event.AuthEvent;
import com.team01.freelance.user.repository.AuthEventRepository;
import com.team01.freelance.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivityFeedServiceTest {

    private static final String OWNER_TOKEN = "owner-token";
    private static final String ADMIN_TOKEN = "admin-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthEventRepository authEventRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MongoDocumentAdapter mongoDocumentAdapter;

    @InjectMocks
    private UserService userService;

    private void stubRedisOps() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getUserActivityFeed_ownerReadsMongoNewestFirstAndCachesResponse() throws Exception {
        stubRedisOps();

        Long userId = 10L;
        String header = "Bearer " + OWNER_TOKEN;

        AuthEvent event = new AuthEvent();
        event.setId("event-1");
        event.setUserId(userId);
        event.setAction("LOGGED_IN");
        event.setTimestamp(LocalDateTime.of(2026, 1, 1, 12, 0));
        event.setDetails(Map.of("ip", "127.0.0.1"));

        UserActivityEventDTO eventDto = UserActivityEventDTO.builder()
                .id("event-1")
                .userId(userId)
                .action("LOGGED_IN")
                .timestamp(event.getTimestamp())
                .details(event.getDetails())
                .build();

        Page<AuthEvent> mongoPage = new PageImpl<>(
                List.of(event),
                PageRequest.of(0, 2),
                1
        );

        when(jwtService.isTokenValid(OWNER_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(OWNER_TOKEN)).thenReturn(userId);
        when(jwtService.extractRole(OWNER_TOKEN)).thenReturn("CLIENT");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(valueOperations.get("user-service::S1-F12::10::page=0::size=2")).thenReturn(null);
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class)))
                .thenReturn(mongoPage);
        when(mongoDocumentAdapter.adapt(event)).thenReturn(eventDto);
        when(objectMapper.writeValueAsString(any(UserActivityFeedDTO.class))).thenReturn("{\"ok\":true}");

        UserActivityFeedDTO result = userService.getUserActivityFeed(userId, 0, 2, header);

        assertEquals(1, result.getContent().size());
        assertEquals("LOGGED_IN", result.getContent().get(0).getAction());
        assertEquals(0, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(1, result.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(authEventRepository).findByUserIdOrderByTimestampDesc(eq(userId), pageableCaptor.capture());

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(2, pageableCaptor.getValue().getPageSize());

        verify(valueOperations).set(
                eq("user-service::S1-F12::10::page=0::size=2"),
                eq("{\"ok\":true}"),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void getUserActivityFeed_returnsCachedResponseWithoutMongoQuery() throws Exception {
        stubRedisOps();

        Long userId = 10L;
        String header = "Bearer " + OWNER_TOKEN;
        String cacheKey = "user-service::S1-F12::10::page=0::size=10";
        String cachedJson = "{\"cached\":true}";

        UserActivityFeedDTO cachedResponse = UserActivityFeedDTO.builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(jwtService.isTokenValid(OWNER_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(OWNER_TOKEN)).thenReturn(userId);
        when(jwtService.extractRole(OWNER_TOKEN)).thenReturn("CLIENT");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(valueOperations.get(cacheKey)).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, UserActivityFeedDTO.class)).thenReturn(cachedResponse);

        UserActivityFeedDTO result = userService.getUserActivityFeed(userId, null, null, header);

        assertSame(cachedResponse, result);
        verify(authEventRepository, never()).findByUserIdOrderByTimestampDesc(any(), any());
    }

    @Test
    void getUserActivityFeed_capsSizeAtOneHundredAndNormalizesNegativePage() throws Exception {
        stubRedisOps();

        Long userId = 10L;
        String header = "Bearer " + OWNER_TOKEN;

        when(jwtService.isTokenValid(OWNER_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(OWNER_TOKEN)).thenReturn(userId);
        when(jwtService.extractRole(OWNER_TOKEN)).thenReturn("CLIENT");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(valueOperations.get("user-service::S1-F12::10::page=0::size=100")).thenReturn(null);
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 100)));
        when(objectMapper.writeValueAsString(any(UserActivityFeedDTO.class))).thenReturn("{}");

        userService.getUserActivityFeed(userId, -5, 500, header);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(authEventRepository).findByUserIdOrderByTimestampDesc(eq(userId), pageableCaptor.capture());

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getUserActivityFeed_adminCanReadAnotherUsersFeed() throws Exception {
        stubRedisOps();

        Long requestedUserId = 10L;
        String header = "Bearer " + ADMIN_TOKEN;

        when(jwtService.isTokenValid(ADMIN_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(ADMIN_TOKEN)).thenReturn(99L);
        when(jwtService.extractRole(ADMIN_TOKEN)).thenReturn("ADMIN");
        when(userRepository.existsById(requestedUserId)).thenReturn(true);
        when(valueOperations.get("user-service::S1-F12::10::page=0::size=10")).thenReturn(null);
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(requestedUserId), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));
        when(objectMapper.writeValueAsString(any(UserActivityFeedDTO.class))).thenReturn("{}");

        UserActivityFeedDTO result = userService.getUserActivityFeed(requestedUserId, 0, 10, header);

        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        verify(authEventRepository).findByUserIdOrderByTimestampDesc(eq(requestedUserId), any(Pageable.class));
    }

    @Test
    void getUserActivityFeed_forbiddenWhenCallerIsNotOwnerOrAdmin() {
        Long requestedUserId = 10L;
        String header = "Bearer " + OWNER_TOKEN;

        when(jwtService.isTokenValid(OWNER_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(OWNER_TOKEN)).thenReturn(99L);
        when(jwtService.extractRole(OWNER_TOKEN)).thenReturn("CLIENT");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserActivityFeed(requestedUserId, 0, 10, header)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).existsById(any());
        verify(authEventRepository, never()).findByUserIdOrderByTimestampDesc(any(), any());
    }

    @Test
    void getUserActivityFeed_unauthorizedWhenTokenMissing() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserActivityFeed(10L, 0, 10, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(authEventRepository, never()).findByUserIdOrderByTimestampDesc(any(), any());
    }

    @Test
    void getUserActivityFeed_notFoundWhenUserDoesNotExist() {
        Long userId = 10L;
        String header = "Bearer " + OWNER_TOKEN;

        when(jwtService.isTokenValid(OWNER_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(OWNER_TOKEN)).thenReturn(userId);
        when(jwtService.extractRole(OWNER_TOKEN)).thenReturn("CLIENT");
        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserActivityFeed(userId, 0, 10, header)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(authEventRepository, never()).findByUserIdOrderByTimestampDesc(any(), any());
    }
}