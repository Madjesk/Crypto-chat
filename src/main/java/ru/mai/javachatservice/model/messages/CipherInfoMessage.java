package ru.mai.javachatservice.model.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.model.client.CipherInfo;
import ru.mai.javachatservice.model.client.RoomInfo;

@Data
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CipherInfoMessage {
    private static final ObjectMapper mapper = new ObjectMapper();
    private String typeMessage = "cipher_info";
    private long anotherClientId;
    private String nameAlgorithm;
    private String namePadding;
    private String encryptionMode;
    private int sizeKeyInBits;
    private int sizeBlockInBits;
    private byte[] initializationVector;
    private byte[] publicKey;
    private byte[] p;
    private byte[] g;

    public CipherInfoMessage(long anotherClientId, CipherInfo cipherInfo, RoomInfo roomInfo) {
        this.anotherClientId = anotherClientId;
        this.nameAlgorithm = cipherInfo.getNameAlgorithm();
        this.namePadding = cipherInfo.getNamePadding();
        this.encryptionMode = cipherInfo.getEncryptionMode();
        this.sizeKeyInBits = cipherInfo.getSizeKeyInBits();
        this.sizeBlockInBits = cipherInfo.getSizeBlockInBits();
        this.initializationVector = cipherInfo.getInitializationVector();
        this.p = roomInfo.getP();
        this.g = roomInfo.getG();
    }

    public byte[] toBytes() {
        try {
            return mapper.writeValueAsString(this).getBytes();
        } catch (JsonProcessingException ex) {
            log.error("Error while processing message to json bytes");
        }

        return new byte[0];
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            log.error("Error while processing message to json bytes");
        }

        return "";
    }
}