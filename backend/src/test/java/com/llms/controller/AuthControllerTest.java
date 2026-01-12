package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.LoginRequest;
import com.llms.dto.request.RegisterRequest;
import com.llms.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Integration Tests")
class AuthControllerTest extends BaseIntegrationTest {

    private static final String AUTH_BASE_URL = "/api/v1/auth";

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("New User")
                    .email("newuser@test.com")
                    .password("password123")
                    .role(UserRole.LENDER)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.role", is("LENDER")))
                    .andExpect(jsonPath("$.name", is("New User")));
        }

        @Test
        @DisplayName("Should register admin user successfully")
        void shouldRegisterAdminUser() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Admin New")
                    .email("adminnew@test.com")
                    .password("password123")
                    .role(UserRole.ADMIN)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")));
        }

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldFailWhenEmailExists() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Duplicate User")
                    .email("lender@test.com") // Already exists from BaseIntegrationTest
                    .password("password123")
                    .role(UserRole.LENDER)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode", is("DUPLICATE")));
        }

        @Test
        @DisplayName("Should fail with invalid email format")
        void shouldFailWithInvalidEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Invalid Email User")
                    .email("invalid-email")
                    .password("password123")
                    .role(UserRole.LENDER)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Should fail with short password")
        void shouldFailWithShortPassword() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("Short Password User")
                    .email("shortpwd@test.com")
                    .password("short")
                    .role(UserRole.LENDER)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Should fail when name is blank")
        void shouldFailWhenNameIsBlank() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .name("")
                    .email("blankname@test.com")
                    .password("password123")
                    .role(UserRole.LENDER)
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("lender@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.role", is("LENDER")))
                    .andExpect(jsonPath("$.name", is("Lender User")));
        }

        @Test
        @DisplayName("Should fail with invalid password")
        void shouldFailWithInvalidPassword() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("lender@test.com")
                    .password("wrongpassword")
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Should fail with non-existent email")
        void shouldFailWithNonExistentEmail() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should fail with blank email")
        void shouldFailWithBlankEmail() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("")
                    .password("password123")
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with blank password")
        void shouldFailWithBlankPassword() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("lender@test.com")
                    .password("")
                    .build();

            mockMvc.perform(post(AUTH_BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
