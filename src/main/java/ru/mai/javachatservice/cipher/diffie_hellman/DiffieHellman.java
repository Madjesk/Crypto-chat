package ru.mai.javachatservice.cipher.diffie_hellman;

import java.math.BigInteger;
import java.util.Random;

public class DiffieHellman {
    private static final int[] VALUES_PRIMITIVE = new int[]{2, 3, 5, 7, 11, 13, 17};
    private static final Random RANDOM_ITEM = new Random();

    private DiffieHellman() {}

    public static BigInteger[] generateParameters(int bitLength) {
        BigInteger g = BigInteger.valueOf(VALUES_PRIMITIVE[RANDOM_ITEM.nextInt(7)]);
        BigInteger p;

        do {
            p = BigInteger.probablePrime(bitLength, RANDOM_ITEM);
        } while (!g.modPow(p.subtract(BigInteger.ONE), p).equals(BigInteger.ONE));

        return new BigInteger[]{p, g};
    }
}
