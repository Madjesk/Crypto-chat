package ru.mai.javachatservice.model.messages.json_parser;

import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.cipher.SymmetricEncryption;
import ru.mai.javachatservice.cipher.cipher_impl.RC6;
import ru.mai.javachatservice.cipher.cipher_impl.Serpent;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.model.messages.CipherInfoMessage;

import java.math.BigInteger;
import java.util.Arrays;

import static ru.mai.javachatservice.cipher.utils.BinaryOperations.byteToIntArray;

@Slf4j
public class CipherInfoMessageParser {
    private static final String UNEXPECTED_VALUE = "Unexpected value: ";

    private CipherInfoMessageParser() {
    }
    public static SymmetricEncryption getCipher(CipherInfoMessage cipherInfo, BigInteger privateKey, BigInteger modulo) {
        byte[] key = getKey(cipherInfo.getPublicKey(), cipherInfo.getSizeKeyInBits(), privateKey, modulo);
        byte[] initializationVector = cipherInfo.getInitializationVector();

        log.info(Arrays.toString(key));

        CipherAlgorithms cipherAlgorithms = getCipherService(
                cipherInfo.getNameAlgorithm(),
                key,
                cipherInfo.getSizeKeyInBits(),
                cipherInfo.getSizeBlockInBits()
        );

        return new SymmetricEncryption(
                getEncryptionMode(cipherInfo.getEncryptionMode()),
                getPadding(cipherInfo.getNamePadding()),
                cipherAlgorithms,
                initializationVector
        );
    }

    public static byte[] getKey(byte[] publicKey, int sizeKeyInBits, BigInteger privateKey, BigInteger modulo) {
        BigInteger publicKeyNumber = new BigInteger(publicKey);
        BigInteger key = publicKeyNumber.modPow(privateKey, modulo);
        byte[] keyBytes = key.toByteArray();
        byte[] result = new byte[sizeKeyInBits / Byte.SIZE];
        System.arraycopy(keyBytes, 0, result, 0, sizeKeyInBits / Byte.SIZE);
        return result;
    }

    public static CipherAlgorithms getCipherService(String nameAlgorithm, byte[] key, int sizeKeyInBits, int sizeBlockInBits) {
        return switch (nameAlgorithm) {
            case "SERPENT" -> new Serpent(sizeKeyInBits, byteToIntArray(key));
            case "RC6" -> new RC6(key);
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + nameAlgorithm);
        };

    }

    public static SymmetricEncryption.PaddingMode getPadding(String namePadding) {
        return switch (namePadding) {
            case "ANSI_X923" -> SymmetricEncryption.PaddingMode.ANSI_X923;
            case "ISO_10126" -> SymmetricEncryption.PaddingMode.ISO_10126;
            case "PKCS7" -> SymmetricEncryption.PaddingMode.PKCS7;
            case "ZEROS" -> SymmetricEncryption.PaddingMode.ZEROS;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + namePadding);
        };
    }

    public static SymmetricEncryption.EncryptionModes getEncryptionMode(String encryptionMode) {
        return switch (encryptionMode) {
            case "ECB" -> SymmetricEncryption.EncryptionModes.ECB;
            case "CBC" -> SymmetricEncryption.EncryptionModes.CBC;
            case "CFB" -> SymmetricEncryption.EncryptionModes.ECB;
            case "CTR" -> SymmetricEncryption.EncryptionModes.CBC;
            case "PCBC" -> SymmetricEncryption.EncryptionModes.PCBC;
            case "RANDOM_DELTA" -> SymmetricEncryption.EncryptionModes.RANDOM_DELTA;
            case "OFB" -> SymmetricEncryption.EncryptionModes.ECB;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + encryptionMode);
        };
    }
}
