package com.team01.freelance.job.controller;

import com.team01.freelance.job.support.AbstractIntegrationTest;
import com.team01.freelance.job.support.cc1.Cc1EndpointScanner;
import com.team01.freelance.job.support.cc1.Cc1EndpointScanner.Endpoint;
import com.team01.freelance.job.support.cc1.Cc1PublicEndpoints;
import com.team01.freelance.user.config.JwtConfig;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;
import com.team01.freelance.user.support.JwtTestSupport;
import com.team01.freelance.user.support.TestAuthHelper;
import com.team01.freelance.user.support.UserTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CC-1 JWT coverage for job-service endpoints. */
@Transactional
class Cc1JwtSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtConfig jwtConfig;

    private MockMvc mockMvc;
    private List<Endpoint> endpoints;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);
        endpoints = Cc1EndpointScanner.scan(applicationContext);
        assertFalse(endpoints.isEmpty());
    }

    @Test
    void allProtectedEndpoints_withoutAuth_return401() throws Exception {
        for (Endpoint endpoint : endpoints) {
            if (!endpoint.isPublic()) {
                mockMvc.perform(Cc1EndpointScanner.mockRequest(endpoint)).andExpect(status().isUnauthorized());
            }
        }
    }

    @Test
    void health_withoutAuth_returns2xx() throws Exception {
        mockMvc.perform(get("/api/jobs/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withMalformedBearer_returns401() throws Exception {
        Endpoint sample = endpoints.stream().filter(e -> !e.isPublic()).findFirst().orElseThrow();
        mockMvc.perform(Cc1EndpointScanner.mockRequest(sample).header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        String expired = JwtTestSupport.expiredToken(admin, jwtConfig);
        Endpoint sample = endpoints.stream().filter(e -> !e.isPublic()).findFirst().orElseThrow();
        mockMvc.perform(Cc1EndpointScanner.mockRequest(sample).header("Authorization", TestAuthHelper.bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointCategories_areOnlyHealth() {
        Set<String> categories = new HashSet<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.isPublic()) {
                categories.add(Cc1PublicEndpoints.category(endpoint.method(), endpoint.path()));
            }
        }
        assertEquals(Set.of("health"), categories);
    }
}
