package ru.mai.javachatservice.cipher.cipher_interface;

public interface CipherAlgorithms {
    int getBlockSize();

    byte[] encryptBlock(byte[] text);

    byte[] decryptBlock(byte[] text);
}
