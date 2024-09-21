package ru.mai.javachatservice.cipher.cipher_impl.padding;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class Padding {
    public byte[] addPadding(byte[] inputText, int requiredBlockSizeInBytes) {
        if (inputText == null) {
            throw new IllegalArgumentException("InputText is null!");
        }
        if (requiredBlockSizeInBytes <= 0) {
            throw new IllegalArgumentException("Illegal inputText block size!");
        }

        byte countBytesPadding = (byte) (requiredBlockSizeInBytes - inputText.length % requiredBlockSizeInBytes);
        byte[] textWithPadding = new byte[inputText.length + countBytesPadding];
        byte[] paddingArray = getArrayPadding(countBytesPadding);

        System.arraycopy(inputText, 0, textWithPadding, 0, inputText.length);
        System.arraycopy(paddingArray, 0, textWithPadding, inputText.length, paddingArray.length);

        return textWithPadding;
    }

    public byte[] removePadding(byte[] inputText) {
        if (inputText == null || inputText.length == 0) {
            throw new IllegalArgumentException("InputText is null or empty!");
        }

        byte countBytesPadding = inputText[inputText.length - 1];

        if (countBytesPadding > inputText.length) {
            throw new IllegalArgumentException("Illegal size padding!");
        }

        byte[] textWithoutPadding = new byte[inputText.length - countBytesPadding];
        System.arraycopy(inputText, 0, textWithoutPadding, 0, inputText.length - countBytesPadding);

        return textWithoutPadding;
    }

    public String addPadding(String pathToFile, int requiredBlockSizeInBytes) throws IOException {
        String fileWithPadding = addPostfixToFileName(pathToFile, "_add_padding");

        try {
            FileUtils.copyFile(new File(pathToFile), new File(fileWithPadding));
        } catch (IOException ex) {
            throw new IOException("Error while copying files!");
        }

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r");
             RandomAccessFile paddingFile = new RandomAccessFile(fileWithPadding, "rw")) {
            paddingFile.seek(inputFile.length());
            byte[] padding = getArrayPadding((byte) (requiredBlockSizeInBytes - inputFile.length() % requiredBlockSizeInBytes));
            paddingFile.write(padding);
        }

        return fileWithPadding;
    }

    public String removePadding(String pathToFile) throws IOException {
        String pathToRemovePaddingFile = addPostfixToFileName(pathToFile, "_remove_padding");
        byte countBytesPadding;
        byte[] buffer;

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r")) {
            inputFile.seek(inputFile.length() - 1);
            countBytesPadding = inputFile.readByte();
            buffer = new byte[(int) (inputFile.length() - countBytesPadding)];
        }

        try (RandomAccessFile inputFile = new RandomAccessFile(pathToFile, "r");
             RandomAccessFile paddingFile = new RandomAccessFile(pathToRemovePaddingFile, "rw")) {
            inputFile.read(buffer);
            paddingFile.write(buffer);
        }

        return pathToRemovePaddingFile;
    }

    private String addPostfixToFileName(String fileName, String postfix) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex);
        return baseName + postfix + extension;
    }

    protected abstract byte[] getArrayPadding(byte countBytesPadding);
}
