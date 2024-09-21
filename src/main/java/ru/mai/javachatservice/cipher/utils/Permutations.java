package ru.mai.javachatservice.cipher.utils;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

@Slf4j
public class Permutations {
    public enum RuleIndex {
        LeftZero,
        LeftOne,
        RightZero,
        RightOne
    }

    public static byte[] permutate(byte[] array, byte[] Pblock, RuleIndex rule) {
        int resultSize = Pblock.length / 8 + (Pblock.length % 8 == 0 ? 0 : 1);
        byte[] result = new byte[resultSize]; // (Pblock.length + 7) / 8;

        for (int i = 0; i < Pblock.length; i++) {
            boolean bitValue = getBit(array, Pblock[i], rule);
            setBit(result, i, bitValue, rule);
        }

        return result;
    }

    private static boolean getBit(byte[] array, byte index, RuleIndex rule) {
        if (!checkBorders(index, array.length, rule))
            throw new IndexOutOfBoundsException("Error number of index: " + index);

        int byteIndex; // индекс в массиве array
        int bitIndex; // номер бита

        // для сведения нумерации с 1 к нумерации с 0
        if (rule == RuleIndex.LeftOne || rule == RuleIndex.RightOne) {
            index--;
        }

        switch (rule) {
            case LeftZero:
            case LeftOne:
                byteIndex = index / 8;
                bitIndex = 7 - index % 8;
                break;
            case RightZero:
            case RightOne:
                byteIndex = (array.length * 8 - 1 - index) / 8;
                bitIndex = index % 8;
                break;
            default:
                throw new IllegalArgumentException("Unknown indexing rule" + rule);
        }

        //boolean temp = ((array[byteIndex] >> bitIndex) & 1) == 1;
        return ((array[byteIndex] >> bitIndex) & 1) == 1;
    }

    // проверка выхода за границы массива байт
    public static boolean checkBorders(int index, int len, RuleIndex rule) {
        len *= 8;
        if (rule == RuleIndex.LeftOne || rule == RuleIndex.RightOne) {
            len++;
        }

        return (index < len && index >= 0);
    }

    private static void setBit(byte[] array, int index, boolean bit, RuleIndex rule) {
        int byteIndex;
        int bitIndex;

        switch (rule) {
            case LeftZero:
            case LeftOne:
                byteIndex = index / 8;
                bitIndex = 7 - index % 8;
                break;
            case RightZero:
            case RightOne:
                byteIndex = (array.length * 8 - 1 - index) / 8;
                bitIndex = 7 - index % 8;
                break;
            default:
                throw new IllegalArgumentException("Unknown indexing rule" + rule);
        }

        if (bit) {
            array[byteIndex] |= (byte) (1 << bitIndex);
        } else {
            array[byteIndex] &= (byte) ~(1 << bitIndex);
        }
        //var temp = array[byteIndex] & 0xFF;
    }


}