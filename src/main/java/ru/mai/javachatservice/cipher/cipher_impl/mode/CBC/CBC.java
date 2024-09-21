package ru.mai.javachatservice.cipher.cipher_impl.mode.CBC;

import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.utils.BinaryOperations;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class CBC implements EncryptionMode, AutoCloseable {
    private final CipherAlgorithms cipherAlgorithm;
    private final byte[] IV;
    private final ExecutorService executorService;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public CBC(CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV, ExecutorService executorService) {
        this.cipherAlgorithm = cipherAlgorithm;
        this.IV = initializationVector_IV;
        this.executorService = executorService;
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[cipherAlgorithm.getBlockSize()]);
    }

    @Override
    public byte[] encrypt(byte[] text) {
        int blockLength = cipherAlgorithm.getBlockSize();
        byte[] result = new byte[text.length];
        byte[] previousBlock = IV;
        int length = text.length / blockLength;

        for (int i = 0; i < length; ++i) {
            int startIndex = i * blockLength;
            byte[] block = new byte[blockLength];
            // byte[] block = threadLocalBuffer.get();
            System.arraycopy(text, startIndex, block, 0, blockLength);

            // шифруем результат XOR текущего блока и результата шифрования предыдущего блока
            byte[] encryptedBlock = cipherAlgorithm.encryptBlock(BinaryOperations.xor(block, previousBlock));
            System.arraycopy(encryptedBlock, 0, result, startIndex, encryptedBlock.length);
            previousBlock = encryptedBlock;
        }

        return result;
    }

    @Override
    public byte[] decrypt(byte[] text) {
        int blockLength = cipherAlgorithm.getBlockSize();
        byte[] result = new byte[text.length];
        int countBlocks = text.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(countBlocks);

        for (int i = 0; i < countBlocks; ++i) {
            final int index = i;

            futures.add(executorService.submit(() -> {
                byte[] previousBlock = (index == 0) ? IV : new byte[blockLength]; // threadLocalBuffer.get()
                if (index != 0) {
                    System.arraycopy(text, (index - 1) * blockLength, previousBlock, 0, blockLength);
                }

                int startIndex = index * blockLength;
                byte[] currentBlock = new byte[blockLength];
                // byte[] currentBlock = threadLocalBuffer.get();
                System.arraycopy(text, startIndex, currentBlock, 0, blockLength);

                // XOR с предыдущим зашифрованным блоком
                byte[] decryptedBlock = BinaryOperations.xor(previousBlock, cipherAlgorithm.decryptBlock(currentBlock));
                System.arraycopy(decryptedBlock, 0, result, startIndex, decryptedBlock.length);
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
            // threadLocalBuffer.remove();
        }
    }
}