package ru.mai.javachatservice.cipher.cipher_impl.mode;

import java.util.concurrent.ExecutionException;

public interface EncryptionMode {
    byte[] encrypt(byte[] text);

    byte[] decrypt(byte[] text);
}
