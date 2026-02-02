package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashSet;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class AuthRestControllerTests {

    @Autowired
    private AuthRestController authRestController;

    @MockBean
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(authRestController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
    }

    @Test
    void testLoginSuccess() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("{noop}admin");
        user.setEnabled(true);
        user.setRoles(new HashSet<>());
        user.addRole("ROLE_ADMIN");

        given(userService.validateCredentials("admin", "admin")).willReturn(true);
        given(userService.findByUsername("admin")).willReturn(user);

        String loginJson = "{\"username\":\"admin\",\"password\":\"admin\"}";

        this.mockMvc.perform(post("/api/auth/login")
                .content(loginJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void testLoginFailureInvalidUsername() throws Exception {
        given(userService.validateCredentials("nonexistent", "admin")).willReturn(false);

        String loginJson = "{\"username\":\"nonexistent\",\"password\":\"admin\"}";

        this.mockMvc.perform(post("/api/auth/login")
                .content(loginJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testLoginFailureInvalidPassword() throws Exception {
        given(userService.validateCredentials("admin", "wrongpassword")).willReturn(false);

        String loginJson = "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";

        this.mockMvc.perform(post("/api/auth/login")
                .content(loginJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testLoginBadRequestMissingCredentials() throws Exception {
        String loginJson = "{}";

        this.mockMvc.perform(post("/api/auth/login")
                .content(loginJson)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}

