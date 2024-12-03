package com.hoit.checkers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hoit.checkers.model.RoomReadyStatus;
import com.hoit.checkers.model.Room;
import com.hoit.checkers.model.User;

public interface RoomReadyStatusRepository extends JpaRepository<RoomReadyStatus, Long> {
    void deleteByRoomAndUser(Room room, User user);
}
