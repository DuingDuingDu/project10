package com.hoit.checkers.repository;

import com.hoit.checkers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CheckersRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByUsernameAndNickname(String username, String nickname);
}
