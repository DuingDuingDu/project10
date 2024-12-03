package com.hoit.checkers.repository;

import com.hoit.checkers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    Optional<User> findByNickname(String nickname); // 반환 타입을 Optional<User>로 수정

    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndNickname(String username, String nickname);
    
}
