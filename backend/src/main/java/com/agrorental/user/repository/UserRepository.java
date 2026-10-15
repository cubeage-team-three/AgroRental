package com.agrorental.user.repository;

import com.agrorental.common.enums.Role;
import com.agrorental.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for managing User entity instances.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseAndRole(String email, Role role);

    Optional<User> findByEmailAndRole(String email, Role role);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);
}
