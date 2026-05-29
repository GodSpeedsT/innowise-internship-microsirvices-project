package com.innowise.authservice.repository;

import com.innowise.authservice.entity.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<AuthUser, UUID> {

  Optional<AuthUser> findByLogin(String login);

  Optional<AuthUser> findByEmail(String email);

  @Query(value = "SELECT EXISTS(SELECT 1 FROM auth_users WHERE login= :login OR email= :email)", nativeQuery = true)
  boolean exitsByLoginOrEmail(@Param("login") String login, @Param("email") String email);

}
