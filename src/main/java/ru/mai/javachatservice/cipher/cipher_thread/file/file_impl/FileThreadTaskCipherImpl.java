package ru.mai.javachatservice.cipher.cipher_thread.file.file_impl;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_interface.FileThreadCipher;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_interface.FileThreadTaskCipher;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class FileThreadTaskCipherImpl implements FileThreadTaskCipher {
    private EncryptionMode encryptionMode;

    @Override
    public byte[] apply(String pathToInputFile, long skipValue, long sizePartBytesThread, boolean encryptOrDecrypt) throws IOException, ExecutionException, InterruptedException {
        byte[] text = new byte[(int) sizePartBytesThread];

        try (RandomAccessFile file = new RandomAccessFile(pathToInputFile, "r")) {
            file.seek(skipValue);
            int countBytes = file.read(text);

            if (countBytes != sizePartBytesThread) {
                byte[] trimText = new byte[countBytes];
                System.arraycopy(text, 0, trimText, 0, countBytes);
                text = trimText;
            }
        } catch (IOException ex) {
            throw new IOException(ex);
        }

        if (encryptOrDecrypt) {
            return encryptionMode.encrypt(text);
        } else {
            return encryptionMode.decrypt(text);
        }
    }
}
