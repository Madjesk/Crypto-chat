package ru.mai.javachatservice.cipher.cipher_impl.padding.impl;

import ru.mai.javachatservice.cipher.cipher_impl.padding.Padding;

import java.util.Arrays;

public class PKCS7 extends Padding {
    @Override
    protected byte[] getArrayPadding(byte countBytesPadding) {
        byte[] padding = new byte[countBytesPadding];
        Arrays.fill(padding, countBytesPadding);
        return padding;
    }
}
