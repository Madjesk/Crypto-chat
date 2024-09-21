package ru.mai.javachatservice.cipher.cipher_impl.padding.impl;

import ru.mai.javachatservice.cipher.cipher_impl.padding.Padding;

import java.util.Random;

public class ISO_10126 extends Padding {
    private final Random random = new Random();

    @Override
    protected byte[] getArrayPadding(byte countBytesPadding) {
        byte[] padding = new byte[countBytesPadding];

        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = (byte) (random.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE) + Byte.MIN_VALUE);
        }

        padding[padding.length - 1] = countBytesPadding;
        return padding;
    }
}
