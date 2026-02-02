package org.springframework.samples.petclinic.repository.userRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link UserRepository} integration tests.
 * Subclasses should specify Spring context configuration using
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration} annotation.
 */
public abstract class AbstractUserRepositoryTests {

    @Autowired
    protected UserRepository userRepository;

    @Test
    void shouldFindUserByUsername() {
        // Test: Find existing user 'admin' from populateDB.sql
        User user = userRepository.findByUsername("admin");

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getRoles()).isNotEmpty();
    }

    @Test
    void shouldReturnNullForNonExistentUsername() {
        // Test: Non-existent username should return null
        User user = userRepository.findByUsername("nonexistent_user");

        assertThat(user).isNull();
    }
}

