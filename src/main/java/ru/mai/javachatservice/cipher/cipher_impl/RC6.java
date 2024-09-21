package ru.mai.javachatservice.cipher.cipher_impl;

import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import java.nio.ByteBuffer;

public class RC6 implements CipherAlgorithms {
    private static final int R = 20;
    private static final int BLOCK_SIZE = 16;

    private static final int P32 = 0xB7E15163;
    private static final int Q32 = 0x9E3779B9;

    private int[] roundKeys;

    public RC6(byte[] key) {
        int lenKeyInBits = key.length * 8;
        if (lenKeyInBits != 128 && lenKeyInBits != 192 && lenKeyInBits != 256) {
            throw new IllegalArgumentException("Error: Invalid key length! Key length must be 128, 192, or 256 bits.");
        }
        this.roundKeys = keyExpansion(key); // Генерация раундовых ключей на основе переданного ключа
    }

    @Override
    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    @Override
    public byte[] encryptBlock(byte[] inputBlock) {
        return encrypt(inputBlock, roundKeys);
    }

    @Override
    public byte[] decryptBlock(byte[] inputBlock) {
        return decrypt(inputBlock, roundKeys);
    }

    private int[] keyExpansion(byte[] key) {
        // Логика расширения ключа
        int[] L = new int[key.length / 4]; // L - массив для расширенного ключа
        for (int i = 0; i < L.length; i++) {
            L[i] = ByteBuffer.wrap(key, i * 4, 4).getInt();
        }
        //раундовые ключи
        int[] S = new int[2 * R + 4];
        S[0] = P32;
        for (int i = 1; i < S.length; i++) {
            S[i] = S[i - 1] + Q32;
        }
        //смешиваем L и S, для равномерного распределения раундовых ключей
        int A = 0, B = 0;
        int i = 0, j = 0;
        int n = 3 * Math.max(L.length, S.length);

        for (int k = 0; k < n; k++) {
            A = S[i] = leftRotate(S[i] + A + B, 3);
            B = L[j] = leftRotate(L[j] + A + B, (A + B));
            i = (i + 1) % S.length;
            j = (j + 1) % L.length;
        }

        return S;
    }

    private byte[] encrypt(byte[] block, int[] S) {
        int A = ByteBuffer.wrap(block, 0, 4).getInt();
        int B = ByteBuffer.wrap(block, 4, 4).getInt();
        int C = ByteBuffer.wrap(block, 8, 4).getInt();
        int D = ByteBuffer.wrap(block, 12, 4).getInt();

        B += S[0];
        D += S[1];

        for (int i = 1; i <= R; i++) {
            int t = leftRotate((B * (2 * B + 1)), 5);
            int u = leftRotate((D * (2 * D + 1)), 5);
            A = leftRotate(A ^ t, u) + S[2 * i];
            C = leftRotate(C ^ u, t) + S[2 * i + 1];

            int temp = A;
            A = B;
            B = C;
            C = D;
            D = temp;
        }

        A += S[2 * R + 2];
        C += S[2 * R + 3];

        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putInt(A).putInt(B).putInt(C).putInt(D);
        return buffer.array();
    }

    private byte[] decrypt(byte[] block, int[] S) {
        int A = ByteBuffer.wrap(block, 0, 4).getInt();
        int B = ByteBuffer.wrap(block, 4, 4).getInt();
        int C = ByteBuffer.wrap(block, 8, 4).getInt();
        int D = ByteBuffer.wrap(block, 12, 4).getInt();

        C -= S[2 * R + 3];
        A -= S[2 * R + 2];

        for (int i = R; i >= 1; i--) {
            int temp = D;
            D = C;
            C = B;
            B = A;
            A = temp;

            int u = leftRotate((D * (2 * D + 1)), 5);
            int t = leftRotate((B * (2 * B + 1)), 5);
            C = rightRotate(C - S[2 * i + 1], t) ^ u;
            A = rightRotate(A - S[2 * i], u) ^ t;
        }

        B -= S[0];
        D -= S[1];

        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putInt(A).putInt(B).putInt(C).putInt(D);
        return buffer.array();
    }

    private int leftRotate(int value, int bits) {
        return (value << bits) | (value >>> (32 - bits));
    }

    private int rightRotate(int value, int bits) {
        return (value >>> bits) | (value << (32 - bits));
    }
}