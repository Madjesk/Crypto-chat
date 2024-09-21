package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.javachatservice.model.client.ClientInfo;

@Repository
public interface ClientRepository extends CrudRepository<ClientInfo, Long> {
    @Transactional
    default ClientInfo addRoom(long clientId, long roomId) {
        ClientInfo client = findById(clientId).orElse(null);

        if (client != null) {
            long[] updatedRooms = new long[client.getRooms().length + 1];
            System.arraycopy(client.getRooms(), 0, updatedRooms, 0, client.getRooms().length);
            updatedRooms[updatedRooms.length - 1] = roomId;

            client.setRooms(updatedRooms);

            return save(client);
        }

        return null;
    }

    @Transactional
    default ClientInfo removeRoom(long clientId, long roomId) {
        ClientInfo client = findById(clientId).orElse(null);

        if (client != null) {
            long[] rooms = client.getRooms();
            long[] updatedRooms = new long[rooms.length - 1];

            int index = 0;

            for (long room : rooms) {
                if (room != roomId) {
                    updatedRooms[index++] = room;
                }
            }

            client.setRooms(updatedRooms);

            return save(client);
        }

        return null;
    }
}
