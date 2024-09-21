package ru.mai.javachatservice.cipher.cipher_impl.mode.ECB;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ECB implements EncryptionMode, AutoCloseable {
    private final CipherAlgorithms cipherAlgorithm;
    private final ExecutorService executorService;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public ECB(CipherAlgorithms cipherAlgorithm, ExecutorService executorService) {
        this.cipherAlgorithm = cipherAlgorithm;
        this.executorService = executorService;
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[cipherAlgorithm.getBlockSize()]);
    }

    @Override
    public byte[] encrypt(byte[] text) {
        return multiprocessingText(text, true);
    }

    @Override
    public byte[] decrypt(byte[] text) {
        return multiprocessingText(text, false);
    }

    private byte[] multiprocessingText(byte[] text, boolean encryptOrDecrypt) {
        byte[] result = new byte[text.length];
        int blockLength = cipherAlgorithm.getBlockSize();
        int countBlocks = text.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(countBlocks);

        for (int i = 0; i < countBlocks; ++i) {
            final int index = i;

            futures.add(executorService.submit(() -> {
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                // byte[] block = threadLocalBuffer.get(); // оптимизация с помощью буфера
                System.arraycopy(text, startIndex, block, 0, blockLength);
                block = encryptOrDecrypt ?
                        cipherAlgorithm.encryptBlock(block) :
                        cipherAlgorithm.decryptBlock(block);
                System.arraycopy(block, 0, result, startIndex, block.length);
            }));
        }

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        return result;
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        } finally {
            //threadLocalBuffer.remove();
        }
    }
}