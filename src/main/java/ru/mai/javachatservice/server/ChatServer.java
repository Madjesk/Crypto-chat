package ru.mai.javachatservice.server;

import com.vaadin.flow.component.UI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.mai.javachatservice.cipher.diffie_hellman.DiffieHellman;
import ru.mai.javachatservice.kafka.KafkaWriter;
import ru.mai.javachatservice.model.client.CipherInfo;
import ru.mai.javachatservice.model.client.ClientInfo;
import ru.mai.javachatservice.model.client.MessageInfo;
import ru.mai.javachatservice.model.client.RoomInfo;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;
import ru.mai.javachatservice.model.messages.Message;
import ru.mai.javachatservice.repository.CipherInfoRepository;
import ru.mai.javachatservice.repository.ClientRepository;
import ru.mai.javachatservice.repository.MessageInfoRepository;
import ru.mai.javachatservice.repository.RoomRepository;

import java.math.BigInteger;
import java.util.*;

@Slf4j
@Service
public class ChatServer {
    private static final Map<String, UI> openWindows = new HashMap<>();
    private static final Map<Long, Pair<Long, Long>> roomConnections = new HashMap<>();
    private final CipherInfoRepository cipherInfoRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;
    private final KafkaWriter kafkaWriter;
    private final MessageInfoRepository messageInfoRepository;

    public ChatServer(MessageInfoRepository messageInfoRepository, CipherInfoRepository cipherInfoRepository, ClientRepository clientRepository, RoomRepository roomRepository, KafkaWriter kafkaWriter) {
        this.messageInfoRepository = messageInfoRepository;
        this.cipherInfoRepository = cipherInfoRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
        this.kafkaWriter = kafkaWriter;
    }

    public List<Long> getAvailableChats(long clientId) {
        Optional<ClientInfo> clientInfoOptional = clientRepository.findById(clientId);

        if (clientInfoOptional.isPresent()) {
            ClientInfo clientInfo = clientInfoOptional.get();
            long[] rooms = clientInfo.getRooms();
            List<Long> availableChats = new ArrayList<>();
            for (long roomId : rooms) {
                availableChats.add(roomId);
            }

            return availableChats;
        }

        return Collections.emptyList();
    }


    public synchronized boolean connectToRoom(long clientId, long roomId) {
        Optional<ClientInfo> clientInfoOptional = clientRepository.findById(clientId);

        if (clientInfoOptional.isEmpty()) {
            return false;
        }

        if (roomConnections.containsKey(roomId)) {
            return handleExistingRoomConnection(clientId, roomId);
        } else {
            return handleNewRoomConnection(clientId, roomId);
        }
    }

    private boolean handleExistingRoomConnection(long clientId, long roomId) {
        Pair<Long, Long> room = roomConnections.get(roomId);

        if (isRoomAvailableForClient(room, clientId)) {
            Long anotherClientId = room.getLeft() == null ? room.getRight() : room.getLeft();
            roomConnections.put(roomId, Pair.of(clientId, anotherClientId));

            ClientInfo updateClient = clientRepository.addRoom(clientId, roomId);
            if (updateClient == null) {
                return false;
            }

            return startRoom(clientId, anotherClientId, roomId);
        }

        return false;
    }

    private boolean isRoomAvailableForClient(Pair<Long, Long> room, long clientId) {
        return (room.getLeft() == null || room.getRight() == null) &&
                !((room.getLeft() != null && room.getLeft() == clientId) || (room.getRight() != null && room.getRight() == clientId));
    }

    private boolean handleNewRoomConnection(long clientId, long roomId) {
        if (!roomRepository.existsRoomInfoByRoomId(roomId)) {
            generateAndSaveRoomParameters(roomId);
        }

        roomConnections.put(roomId, Pair.of(clientId, null));
        clientRepository.addRoom(clientId, roomId);

        return true;
    }

    private void generateAndSaveRoomParameters(long roomId) {
        BigInteger[] roomParameters = DiffieHellman.generateParameters(300);
        byte[] p = roomParameters[0].toByteArray();
        byte[] g = roomParameters[1].toByteArray();

        roomRepository.save(
                RoomInfo.builder()
                        .roomId(roomId)
                        .p(p)
                        .g(g)
                        .build()
        );
    }

public synchronized void disconnectFromRoom(long clientId, long roomId) {
    if (!roomConnections.containsKey(roomId)) {
        return;
    }

    log.info("trying to disconnect");
    Pair<Long, Long> room = roomConnections.get(roomId);

    if (!isClientInRoom(clientId, room)) {
        return;
    }

    updateRoomConnections(clientId, roomId, room);
    closeRoomIfEmpty(roomId);
    closeWindowForClient(clientId, roomId);
    clientRepository.removeRoom(clientId, roomId);
}

    private boolean isClientInRoom(long clientId, Pair<Long, Long> room) {
        return (room.getLeft() != null && room.getLeft() == clientId)
                || (room.getRight() != null && room.getRight() == clientId);
    }

    private void updateRoomConnections(long clientId, long roomId, Pair<Long, Long> room) {
        if (room.getLeft() != null && room.getLeft() == clientId) {
            roomConnections.put(roomId, Pair.of(null, room.getRight()));
        } else if (room.getRight() != null && room.getRight() == clientId) {
            roomConnections.put(roomId, Pair.of(room.getLeft(), null));
        }
    }

    private void closeRoomIfEmpty(long roomId) {
        Pair<Long, Long> updatedRoom = roomConnections.get(roomId);
        if (updatedRoom.getLeft() == null && updatedRoom.getRight() == null) {
            log.info("remove roomId");
            roomConnections.remove(roomId);
        }
    }

    private void closeWindowForClient(long clientId, long roomId) {
        String url = "room/" + clientId + "/" + roomId;
        UI ui = openWindows.get(url);
        if (ui != null) {
            ui.getPage().executeJs("window.close()");
            removeWindow(url);
        }
    }

    private boolean startRoom(long firstClientId, long secondClientId, long roomId) {
        log.info("Starting room with id = " + roomId);
        CipherInfo firstCipherInfo = getCipherInfoById(firstClientId);
        CipherInfo secondCipherInfo = getCipherInfoById(secondClientId);
        RoomInfo roomInfo = getRoomInfoById(roomId);

        String outputTopicFirst = "input_" + secondClientId + "_" + roomId;
        String outputTopicSecond = "input_" + firstClientId + "_" + roomId;

        if (firstCipherInfo != null && secondCipherInfo != null && roomInfo != null) {
            CipherInfoMessage firstMessage = new CipherInfoMessage(firstClientId, firstCipherInfo, roomInfo);
            CipherInfoMessage secondMessage = new CipherInfoMessage(secondClientId, secondCipherInfo, roomInfo);

            kafkaWriter.processing(firstMessage.toBytes(), outputTopicFirst);
            kafkaWriter.processing(secondMessage.toBytes(), outputTopicSecond);

            return true;
        }

        return false;
    }

    public boolean notExistClient(long clientId) {
        return !clientRepository.existsById(clientId);
    }

    public synchronized void saveMessage(long from, long to, Message message) {
        messageInfoRepository.save(MessageInfo.builder()
                .from(from)
                .to(to)
                .message(message)
                .build());
    }

    public CipherInfoMessage getCipherInfoMessageClient(long clientId, long roomId) {
        CipherInfo cipherInfo = getCipherInfoById(clientId);
        RoomInfo roomInfo = getRoomInfoById(roomId);

        if (cipherInfo != null && roomInfo != null) {
            return new CipherInfoMessage(clientId, cipherInfo, roomInfo);
        }

        return null;
    }

    public CipherInfo getCipherInfoById(long clientId) {
        Optional<ClientInfo> clientInfoOptional = clientRepository.findById(clientId);

        if (clientInfoOptional.isPresent()) {
            ClientInfo clientInfo = clientInfoOptional.get();

            return cipherInfoRepository.findById(clientInfo.getIdCipherInfo()).orElse(null);
        }

        return null;
    }

    public RoomInfo getRoomInfoById(long roomId) {
        return roomRepository.getRoomInfoByRoomId(roomId).orElse(null);
    }

    public synchronized void addWindow(String url, UI ui) {
        openWindows.put(url, ui);
    }

    public synchronized void removeWindow(String url) {
        openWindows.remove(url);
    }

    public synchronized boolean isNotOpenWindow(String url) {
        return !openWindows.containsKey(url);
    }

}
