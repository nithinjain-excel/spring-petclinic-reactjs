package org.springframework.samples.petclinic.service.userService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractUserServiceTests {

    @Autowired
    private UserService userService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldAddUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setEnabled(true);
        user.addRole("OWNER_ADMIN");

        userService.saveUser(user);
        assertThat(user.getRoles().parallelStream().allMatch(role -> role.getName().startsWith("ROLE_")), is(true));
        assertThat(user.getRoles().parallelStream().allMatch(role -> role.getUser() != null), is(true));
    }

    @Test
    public void shouldFindUserByUsername() {
        User user = userService.findByUsername("admin");

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    public void shouldReturnTrueForValidCredentials() {
        boolean result = userService.validateCredentials("admin", "admin");

        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForInvalidUsername() {
        boolean result = userService.validateCredentials("nonexistent", "admin");

        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseForInvalidPassword() {
        boolean result = userService.validateCredentials("admin", "wrongpassword");

        assertThat(result).isFalse();
    }
}
