package com.hoit.checkers.repository;

import com.hoit.checkers.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, String> {
    // 추가적인 쿼리 메서드 필요 시 정의
}
