package ru.mai.javachatservice.cipher.cipher_impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.cipher.cipher_impl.SerpentService.*;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.utils.Permutations;

import java.util.Arrays;

import static ru.mai.javachatservice.cipher.cipher_impl.SerpentService.FP;
import static ru.mai.javachatservice.cipher.cipher_impl.SerpentService.IP;

@Slf4j
public class Serpent implements CipherAlgorithms {
    private final SerpentService serpentService;
    private final byte[][] roundKeys;
    private final int[] key;

    public Serpent(int lenKeyInBits, int[] key) {
        if (!(lenKeyInBits == 128 || lenKeyInBits == 192 ||lenKeyInBits == 256)) {
            throw new IllegalArgumentException("Error len key in bits!");
        }

        this.key = Arrays.copyOf(key, key.length);
        this.serpentService = new SerpentService(lenKeyInBits);
        this.roundKeys = serpentService.expandKey(this.key);
    }

    @Override
    public int getBlockSize() {
        return 16; // 128 бит, 16 байт
    }

    @Override
    public byte[] encryptBlock(byte[] inputBlock) {
        byte[] encryptBlock = Permutations.permutate(inputBlock, IP, Permutations.RuleIndex.LeftZero);

        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 16; j++) {
                encryptBlock[j] = (byte) (roundKeys[i][j] ^ encryptBlock[j]);
            }

            //заменяем каждую половину байта с помощью s блока
            for (int j = 0; j < 16; j++) {
                byte tempL = (byte) (encryptBlock[j] & 0x0f);
                byte tempR = (byte) ((encryptBlock[j] & 0xf0) >> 4);
                encryptBlock[j] = (byte) ((serpentService.replaceFromSBox(tempL, i % 8) << 4) | serpentService.replaceFromSBox(tempR, i % 8) & 0xff);
            }

            if (i != 31) {
                serpentService.linearTransform(encryptBlock);
            } else {
                for (int j = 0; j < 16; j++) {
                    encryptBlock[j] = (byte) (roundKeys[i + 1][j] ^ encryptBlock[j]);
                }
            }
        }

        encryptBlock = Permutations.permutate(encryptBlock, FP, Permutations.RuleIndex.LeftZero);
        return encryptBlock;
    }

    @Override
    public byte[] decryptBlock(byte[] inputBlock) {
        inputBlock = Permutations.permutate(inputBlock, IP, Permutations.RuleIndex.LeftZero);
        for (int i = 31; i >= 0; i--) {
            if (i == 31) {
                for (int j = 0; j < 16; j++) {
                    inputBlock[j] = (byte) (roundKeys[i + 1][j] ^ inputBlock[j]);
                }
            } else {
                serpentService.inverseLinearTransform(inputBlock);
            }

            for (int j = 0; j < 16; j++) {
                byte tempL = (byte) (inputBlock[j] & 0x0f);
                byte tempR = (byte) ((inputBlock[j] & 0xf0) >> 4);
                inputBlock[j] = (byte) ((serpentService.replaceFromInverseSBox(tempL, i % 8) << 4) | serpentService.replaceFromInverseSBox(tempR, i % 8) & 0xff);
            }

            for (int j = 0; j < 16; j++) {
                inputBlock[j] = (byte) (roundKeys[i][j] ^ inputBlock[j]);
            }
        }

        inputBlock = Permutations.permutate(inputBlock, FP, Permutations.RuleIndex.LeftZero);
        return inputBlock;
    }
}
