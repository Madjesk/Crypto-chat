package ru.mai.javachatservice.cipher.cipher_thread.file.file_interface;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface FileThreadCipher {
    String cipher(String pathToInputFile, String pathToOutputFile, boolean encryptOrDecrypt) throws IOException, ExecutionException, InterruptedException;
}

