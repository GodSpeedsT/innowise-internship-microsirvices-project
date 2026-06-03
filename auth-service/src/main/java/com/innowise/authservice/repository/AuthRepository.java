package com.innowise.authservice.repository;

import com.innowise.authservice.entity.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<AuthUser, UUID> {

  Optional<AuthUser> findByLogin(String login);

  boolean existsByLoginOrEmail(String login, String email);

}
