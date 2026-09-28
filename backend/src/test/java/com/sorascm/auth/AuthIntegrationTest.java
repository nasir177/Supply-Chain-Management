package com.sorascm.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorascm.auth.domain.RefreshToken;
import com.sorascm.auth.domain.RoleType;
import com.sorascm.auth.dto.AuthDtos.*;
import com.sorascm.auth.repository.RefreshTokenRepository;
import com.sorascm.auth.repository.UserRepository;
import com.sorascm.base.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user and hash password")
    void registerUser_Success() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "john_doe",
                "john.doe@sorascm.com",
                "StrongPassword123!",
                "John",
                "Doe",
                Set.of(RoleType.ROLE_OPERATOR)
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("john_doe"));

        var savedUser = userRepository.findByUsername("john_doe").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("StrongPassword123!");
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @DisplayName("Should authenticate user and provide valid tokens")
    void loginUser_Success() throws Exception {
        registerTestUser("soramanager", "mgr@sorascm.com", "Password123!", Set.of(RoleType.ROLE_WAREHOUSE_MANAGER));

        LoginRequest loginReq = new LoginRequest("soramanager", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void loginUser_BadCredentials() throws Exception {
        registerTestUser("soramanager", "mgr@sorascm.com", "Password123!", Set.of(RoleType.ROLE_WAREHOUSE_MANAGER));

        LoginRequest loginReq = new LoginRequest("soramanager", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should enforce RBAC permissions correctly via JWT claims")
    void rbacEnforcement_Success() throws Exception {
        String operatorToken = registerAndGetAccessToken("operator_user", "op@sorascm.com", Set.of(RoleType.ROLE_OPERATOR));

        // Operator accessing general authenticated endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/test/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk());

        // Operator accessing ADMIN endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        String adminToken = registerAndGetAccessToken("admin_user", "admin@sorascm.com", Set.of(RoleType.ROLE_ADMIN));

        // Admin accessing ADMIN endpoint -> 200 OK
        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("CONFIDENTIAL_METRICS"));
    }

    @Test
    @DisplayName("Should rotate refresh token on use and invalidate old token")
    void refreshTokenRotation_Success() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "rotation_user",
                "rotate@sorascm.com",
                "Password123!",
                "Rotate",
                "User",
                Set.of(RoleType.ROLE_OPERATOR)
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String oldRefreshToken = jsonNode.get("data").get("refreshToken").asText();

        // Rotate token
        TokenRefreshRequest refreshReq = new TokenRefreshRequest(oldRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJsonNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJsonNode.get("data").get("refreshToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.stream().filter(RefreshToken::isRevoked).count()).isEqualTo(1);

        // Reusing the old token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest(oldRefreshToken))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void registerTestUser(String username, String email, String password, Set<RoleType> roles) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(username, email, password, "Test", "User", roles);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());
    }

    private String registerAndGetAccessToken(String username, String email, Set<RoleType> roles) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(username, email, "Password123!", "First", "Last", roles);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("data").get("accessToken").asText();
    }
}