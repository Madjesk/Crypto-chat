package ru.mai.javachatservice.authorization;

import org.springframework.stereotype.Component;
import ru.mai.javachatservice.model.client.CipherInfo;
import ru.mai.javachatservice.model.client.ClientInfo;
import ru.mai.javachatservice.repository.CipherInfoRepository;
import ru.mai.javachatservice.repository.ClientRepository;

import java.util.Random;

@Component
public class Authorization {
    private static final Random RANDOM = new Random();

    private final CipherInfoRepository cipherInfoRepository;
    private final ClientRepository clientRepository;


    public Authorization(CipherInfoRepository cipherInfoRepository, ClientRepository clientRepository) {
        this.cipherInfoRepository = cipherInfoRepository;
        this.clientRepository = clientRepository;
    }

    public synchronized ClientInfo authorize(String name, String nameAlgorithm, String namePadding, String encryptionMode) {
        CipherInfo cipherInfo = cipherInfoRepository.save(getCipherInfo(nameAlgorithm, namePadding, encryptionMode));
        return clientRepository.save(ClientInfo.
                builder()
                .name(name)
                .idCipherInfo(cipherInfo.getId())
                .rooms(new long[0])
                .build()
        );
    }

    private CipherInfo getCipherInfo(String nameAlgorithm, String namePadding, String encryptionMode) {
        return switch (nameAlgorithm) {
            case "SERPENT" -> CipherInfo.builder()
                    .nameAlgorithm("SERPENT")
                    .namePadding(namePadding)
                    .encryptionMode(encryptionMode)
                    .sizeKeyInBits(128)
                    .sizeBlockInBits(128)
                    .initializationVector(generateInitVector(16))
                    .build();
            case "RC6" -> CipherInfo.builder()
                    .nameAlgorithm("RC6")
                    .namePadding(namePadding)
                    .encryptionMode(encryptionMode)
                    .sizeKeyInBits(128)
                    .sizeBlockInBits(128)
                    .initializationVector(generateInitVector(16))
                    .build();
            default -> throw new IllegalStateException("Unknow algorithms: " + nameAlgorithm);
        };
    }

    private byte[] generateInitVector(int size) {
        byte[] vector = new byte[size];

        for (int i = 0; i < size; i++) {
            vector[i] = (byte) RANDOM.nextInt(128);
        }

        return vector;
    }
}
