package ru.mai.javachatservice.cipher.cipher_thread.file.file_impl;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_interface.FileThreadCipher;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_interface.FileThreadTaskCipher;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@AllArgsConstructor
public class FileThreadCipherImpl implements FileThreadCipher {
    private int sizeBlockBytes;
    private FileThreadTaskCipher fileThreadTaskCipher;

    @Override
    public String cipher(String pathToInputFile, String pathToOutputFile, boolean encryptOrDecrypt) throws IOException, ExecutionException, InterruptedException {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        List<Future<byte[]>> futures = new ArrayList<>();
        ExecutorService service = Executors.newFixedThreadPool(availableProcessors);

        try (RandomAccessFile file = new RandomAccessFile(pathToInputFile, "r")) {
            long skipValue = 0;
            long sizePartsThread = ((file.length() / sizeBlockBytes) + availableProcessors - 1) / availableProcessors;
            long sizePartBytesThread = sizePartsThread * sizeBlockBytes;

            while (skipValue < file.length()) {
                long finalSkipValue = skipValue;
                futures.add(service.submit(() -> fileThreadTaskCipher.apply(pathToInputFile, finalSkipValue, sizePartBytesThread, encryptOrDecrypt)));
                skipValue += sizePartBytesThread;
            }

        } catch (IOException ex) {
            throw new IOException(ex);
        }

        try (RandomAccessFile file = new RandomAccessFile(pathToOutputFile, "rw")) {
            for (Future<byte[]> future : futures) {
                byte[] text = future.get();
                file.write(text);
            }
        } catch (IOException ex) {
            throw new IOException(ex);
        } catch (InterruptedException ex) {
            throw new InterruptedException(ex.getMessage());
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex);
        }

        service.shutdown();

        try {
            if (!service.awaitTermination(2, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return pathToOutputFile;
    }
}