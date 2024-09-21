package ru.mai.javachatservice.cipher.cipher_impl.padding.impl;

import ru.mai.javachatservice.cipher.cipher_impl.padding.Padding;

public class ANSI_X923 extends Padding {
    @Override
    protected byte[] getArrayPadding(byte countBytesPadding) {
        byte[] padding = new byte[countBytesPadding];

        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = 0;
        }

        padding[padding.length - 1] = countBytesPadding;
        return padding;
    }
}
