package com.campuslab.user;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the UserRepository interface.
 * Tests the repository interface definition and method signatures.
 * 
 * Requirements: 1.1, 1.4
 */
class UserRepositoryTest {

    @Test
    void shouldExtendJpaRepository() {
        // Given & When & Then
        assertThat(JpaRepository.class).isAssignableFrom(UserRepository.class);
    }

    @Test
    void shouldHaveFindByEmailMethod() throws NoSuchMethodException {
        // Given & When
        Method method = UserRepository.class.getMethod("findByEmail", String.class);
        
        // Then
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(Optional.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(String.class);
    }

    @Test
    void shouldUseCorrectGenericTypes() {
        // Given & When
        Class<?>[] interfaces = UserRepository.class.getInterfaces();
        
        // Then
        assertThat(interfaces).hasSize(1);
        assertThat(interfaces[0]).isEqualTo(JpaRepository.class);
        
        // Verify that UserRepository works with User entity and UUID as ID type
        // This is implicitly tested by the compiler, but we can verify the method signatures
        assertThat(UserRepository.class.getMethods())
            .extracting(Method::getName)
            .contains("save", "findById", "findAll", "deleteById", "findByEmail");
    }

    @Test
    void shouldBeAnInterface() {
        // Given & When & Then
        assertThat(UserRepository.class.isInterface()).isTrue();
    }
}