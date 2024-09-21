package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.mai.javachatservice.model.client.RoomInfo;

import java.util.Optional;

@Repository
public interface RoomRepository extends CrudRepository<RoomInfo, Long> {
    Optional<RoomInfo> getRoomInfoByRoomId(long roomId);

    boolean existsRoomInfoByRoomId(long roomId);
}
