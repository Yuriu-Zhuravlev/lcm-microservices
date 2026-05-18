package com.yurii.zhuravlov.authservice.repository;

import com.yurii.zhuravlov.authservice.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        User user = new User();
        user.setUsername("test_user");
        user.setPassword("secret");
        user.setRoles(Set.of("ROLE_USER"));

        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByUsername("test_user");

        assertTrue(found.isPresent());
        assertEquals("test_user", found.get().getUsername());
        assertEquals(1, found.get().getRoles().size());
        assertTrue(found.get().getRoles().contains("ROLE_USER"));
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> found = userRepository.findByUsername("non_existent");

        assertTrue(found.isEmpty());
    }

    @Test
    void saveUser_ShouldFail_WhenUsernameNotUnique() {
        User user1 = new User();
        user1.setUsername("unique");
        user1.setPassword("pass");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("unique");
        user2.setPassword("pass2");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
    }
}