package org.springframework.samples.petclinic.repository.userRepository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test using the 'Spring Data JPA' profile.
 *
 * @see AbstractUserRepositoryTests for more details.
 */
@SpringBootTest
@ActiveProfiles({"spring-data-jpa", "hsqldb"})
class UserRepositorySpringDataJpaTests extends AbstractUserRepositoryTests {

}

