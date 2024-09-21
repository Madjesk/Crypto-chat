package ru.mai.javachatservice.cipher.cipher_impl;

import ru.mai.javachatservice.cipher.utils.BinaryOperations;
import ru.mai.javachatservice.cipher.utils.Permutations;

public class SerpentService {
    public static byte[] IP = {
            0, 32, 64, 96, 1, 33, 65, 97, 2, 34, 66, 98, 3, 35, 67, 99,
            4, 36, 68, 100, 5, 37, 69, 101, 6, 38, 70, 102,	7, 39, 71, 103,
            8, 40, 72, 104, 9, 41, 73, 105, 10, 42, 74, 106, 11, 43, 75, 107,
            12,	44,	76,	108, 13, 45, 77, 109, 14, 46, 78, 110, 15, 47, 79, 111,
            16,	48,	80,	112, 17, 49, 81, 113, 18, 50, 82, 114, 19, 51, 83, 115,
            20,	52,	84,	116, 21, 53, 85, 117, 22, 54, 86, 118, 23, 55, 87, 119,
            24,	56,	88,	120, 25, 57, 89, 121, 26, 58, 90, 122, 27, 59, 91, 123,
            28,	60,	92,	124, 29, 61, 93, 125, 30, 62, 94, 126, 31, 63, 95, 127
    };

    public static byte[][] S_BOX_TABLE = {
            {3, 8, 15, 1, 10, 6, 5, 11, 14, 13, 4, 2, 7, 0, 9, 12},
            {15, 12, 2, 7, 9, 0, 5, 10, 1, 11, 14, 8, 6, 13, 3, 4},
            {8, 6, 7, 9, 3, 12, 10, 15, 13, 1, 14, 4, 0, 11, 5, 2},
            {0, 15, 11, 8, 12, 9, 6, 3, 13, 1, 2, 4, 10, 7, 5, 14},
            {1, 15, 8, 3, 12, 0, 11, 6, 2, 5, 4, 10, 9, 14, 7, 13},
            {15, 5, 2, 11, 4, 10, 9, 12, 0, 3, 14, 8, 13, 6, 7, 1},
            {7, 2, 12, 5, 8, 4, 6, 11, 14, 9, 1, 15, 13, 3, 10, 0},
            {1, 13, 15, 0, 14, 8, 2, 11, 7, 4, 12, 10, 9, 3, 5, 6}
    };

    public static byte[][] S_BOX_INVERSE_TABLE = {
            {13, 3,	11,	0, 10, 6, 5, 12, 1, 14, 4, 7, 15, 9, 8, 2},
            {5,	8, 2, 14, 15, 6, 12, 3,	11,	4, 7, 9, 1,	13,	10,	0},
            {12, 9,	15,	4, 11, 14, 1, 2, 0,	3, 6, 13, 5, 8, 10, 7},
            {0,	9, 10, 7, 11, 14, 6, 13, 3,	5, 12, 2, 4, 8,	15,	1},
            {5,	0, 8, 3, 10, 9, 7, 14, 2, 12, 11, 6, 4,	15,	13,	1},
            {8, 15, 2, 9, 4, 1, 13, 14, 11, 6, 5, 3, 7, 12, 10, 0},
            {15, 10, 1,	13,	5, 3, 6, 0,	4, 9, 14, 7, 2,	12,	8, 11},
            {3, 0, 6, 13, 9, 14, 15, 8,	5, 12, 11, 7, 10, 1, 4,	2}
    };

    public static byte[] FP = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60,
            64, 68, 72, 76, 80, 84, 88, 92, 96, 100, 104, 108, 112, 116, 120, 124,
            1, 5, 9, 13, 17, 21, 25, 29, 33, 37, 41, 45, 49, 53, 57, 61,
            65, 69, 73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125,
            2, 6, 10, 14, 18, 22, 26, 30, 34, 38, 42, 46, 50, 54, 58, 62,
            66, 70, 74, 78, 82, 86, 90, 94, 98, 102, 106, 110, 114, 118, 122, 126,
            3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59, 63,
            67, 71, 75, 79, 83, 87, 91, 95, 99, 103, 107, 111, 115, 119, 123, 127
    };

    private final int lenKeyInBits;

    public SerpentService(int lenKeyInBits) {
        this.lenKeyInBits = lenKeyInBits;
    }

    public byte[][] expandKey(int[] key) {
        key = paddingKey(key);

        return generateRoundKeys(generatePreRoundKeys(key));
    }

    //дополняем до 32бит
    public int[] paddingKey(int[] key) {
        int[] paddedKey = new int[8];
        System.arraycopy(key, 0, paddedKey, 0, key.length);

        if (key.length < 8) {
            paddedKey[key.length] = 1 << 31; //ст бит
            for (int i = key.length + 1; i < 8; i++) {
                paddedKey[i] = 0;
            }
        }

        return paddedKey;
    }

    public int[] generatePreRoundKeys(int[] key) {
        int goldenRatio = 0x9e3779b9;
        int[] w = new int[132]; // 33 подключа k_i по 4 int

        System.arraycopy(key, 0, w, 0, 8);
        for (int i = 8; i < 132; i++) {
            w[i] = BinaryOperations.leftCycleShift((w[i - 8] ^ w[i - 5] ^ w[i - 3] ^ w[i - 1] ^ goldenRatio ^ (i - 8)),
                    Integer.SIZE, 11);
        }

        return w;
    }

    public byte[][] generateRoundKeys(int[] w) {
        byte[][] keys = new byte[132][4]; // 132 подключа k_i по 4 byte, образующие 33 K_i
        byte[][] K = new byte[33][16]; // 33 раундовых ключа K_i

        int j = 3;
        for (int i = 0; i < 33; i++) {
            for (int k = 4 * i; k < 4 * i + 4; k++) {
                var key_left = Permutations.permutate(new byte[] {(byte) (w[k] >>> 24 & 0xff), (byte) (w[k] >>> 16 & 0x00ff)}, S_BOX_TABLE[j], Permutations.RuleIndex.LeftZero);
                var key_right = Permutations.permutate(new byte[] {(byte) (w[k] >>> 8 & 0x0000ff), (byte) (w[k] & 0x000000ff)}, S_BOX_TABLE[j], Permutations.RuleIndex.LeftZero);
                keys[k][0] = (key_left[0]);
                keys[k][1] = (key_left[1]);
                keys[k][2] = (key_right[0]);
                keys[k][3] = (key_right[1]);
                j--;

                if (j == -1) {
                    j = 7;
                }
            }
        }

        for (int i = 0; i < 33; i++) {
            for (int n = 0; n < 16; n++) {
                K[i][n] = keys[i * 4 + n / 4][n % 4];
            }
        }

        return K;
    }

    public byte replaceFromSBox(byte b, int i) {
        return S_BOX_TABLE[i][b];
    }

    public byte replaceFromInverseSBox(byte b, int i) {
        return S_BOX_INVERSE_TABLE[i][b];
    }

    public void linearTransform(byte[] input) {
        int[] x = new int[4];

        for (int i = 0; i < 4; i++) {
            x[i] = (input[4*i] & 0xff) << 24
                    | (input[4*i + 1]& 0xff) << 16
                    | (input[4*i + 2] & 0xff) << 8
                    | (input[4*i + 3] & 0xff);
        }

        x[0] = ((x[0] << 13) | (x[0] >>> (32 - 13)));
        x[2] = x[2] << 3 | x[2] >>> 29;
        x[1] = x[1] ^ x[0] ^ x[2];
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] << 1 | x[1] >>> 31;
        x[3] = x[3] << 7 | x[3] >>> 25;
        x[0] = x[0] ^ x[1] ^ x[3];
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] << 5 | x[0] >>> 27;
        x[2] = x[2] << 22 | x[2] >>> 10;

        input = BinaryOperations.intToByteArray(x);
    }

    public void inverseLinearTransform(byte[] input) {
        int[] x = new int[4];

        for (int i = 0; i < 4; i++) {
            x[i] = (input[i * 4] & 0xff) << 24 | (input[i * 4 + 1] & 0xff) << 16 | (input[i * 4 + 2] & 0xff) << 8 | (input[i * 4 + 3] & 0xff);
        }

        x[2] = x[2] >>> 22 | x[2] << 10;
        x[0] = x[0] >>> 5 | x[0] << 27;
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] ^ x[1] ^ x[3];
        x[3] = x[3] >>> 7 | x[3] << 25;
        x[1] = x[1] >>> 1 | x[1] << 31;
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] ^ x[0] ^ x[2];
        x[2] = x[2] >>> 3 | x[2] << 29;
        x[0] = x[0] >>> 13 | x[0] << 19;

        input = BinaryOperations.intToByteArray(x);
    }
}
