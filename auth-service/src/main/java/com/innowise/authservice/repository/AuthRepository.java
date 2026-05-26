package com.innowise.authservice.repository;

import com.innowise.authservice.dto.AuthRequest;
import com.innowise.authservice.entity.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<AuthUser, UUID> {

  Optional<AuthUser> findByLogin(String login);
  Optional<AuthUser> findByEmail(String email);
  @Query(value = "SELECT User u FROM auth_users WHERE login= :login OR email= :email",nativeQuery = true)
  Optional<AuthUser> findByEmailOrLogin(String login, String email);
  boolean existsByLogin(String login);

}
