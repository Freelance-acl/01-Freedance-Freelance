package com.team01.freelance.user.controller;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [S1-F1] Integration tests for {@code GET /api/users/search}.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UserSearchIntegrationTest extends AbstractIntegrationTest {

    private static final String SEARCH_URL = "/api/users/search";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private User ahmed;
    private User sara;
    private User ahmedAli;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(webApplicationContext);

        long suffix = System.nanoTime();
        ahmed = saveUser("Ahmed", "ahmed-" + suffix + "@test.dev", "+10001" + (suffix % 1_000_000L),
                UserRole.FREELANCER);
        sara = saveUser("Sara", "sara-" + suffix + "@test.dev", "+10002" + (suffix % 1_000_000L),
                UserRole.CLIENT);
        ahmedAli = saveUser("Ahmed Ali", "ahmedali-" + suffix + "@test.dev", "+10003" + (suffix % 1_000_000L),
                UserRole.FREELANCER);
    }

    /** Spec (b): name=Ahmed → Ahmed and Ahmed Ali. */
    @Test
    void searchByNameAhmed_returnsTwoFreelancers() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("name", "Ahmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Ahmed", "Ahmed Ali")))
                .andExpect(jsonPath("$[*].password").doesNotExist());
    }

    /** Spec (c): role=CLIENT → Sara only. */
    @Test
    void searchByRoleClient_returnsSaraOnly() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sara"))
                .andExpect(jsonPath("$[0].role").value("CLIENT"));
    }

    /** Spec (d): name=xyz → empty list. */
    @Test
    void searchByUnknownName_returnsEmptyList() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /** Spec (e): name=ahmed → case-insensitive, still 2 matches. */
    @Test
    void searchByNameCaseInsensitive_returnsTwoUsers() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("name", "ahmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Ahmed", "Ahmed Ali")));
    }

    @Test
    void searchByEmailPartial_returnsMatchingUser() throws Exception {
        mockMvc.perform(get(SEARCH_URL).param("email", sara.getEmail().substring(0, 8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sara"));
    }

    private User saveUser(String name, String email, String phone, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}