package ru.mai.javachatservice.cipher.cipher_impl.mode.CTR;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.utils.BinaryOperations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CTR implements EncryptionMode, AutoCloseable {
    private final CipherAlgorithms cipherAlgorithm;
    private final byte[] IV;
    private final ExecutorService executorService;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public CTR(CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV, ExecutorService executorService) {
        this.cipherAlgorithm = cipherAlgorithm;
        this.IV = initializationVector_IV;
        this.executorService = executorService;
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[cipherAlgorithm.getBlockSize()]);
    }

    @Override
    public byte[] encrypt(byte[] text) {
        return multiprocessingText(text);
    }

    @Override
    public byte[] decrypt(byte[] text) {
        return multiprocessingText(text);
    }

    private byte[] multiprocessingText(byte[] text) {
        int blockLength = cipherAlgorithm.getBlockSize();
        byte[] result = new byte[text.length];
        int countBlocks = text.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(countBlocks);

        for (int i = 0; i < countBlocks; ++i) {
            final int index = i;

            futures.add(executorService.submit(() -> {
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                // byte[] block = threadLocalBuffer.get();
                System.arraycopy(text, startIndex, block, 0, blockLength);

                byte[] blockForProcess = new byte[blockLength];
                // byte[] blockForProcess = threadLocalBuffer.get();
                int length = blockLength - Integer.BYTES;
                System.arraycopy(IV, 0, blockForProcess, 0, length);

                byte[] counterInBytes = new byte[Integer.BYTES];
                for (int j = 0; j < counterInBytes.length; ++j) {
                    counterInBytes[j] = (byte) (index >> (3 - j) * 8);
                }
                System.arraycopy(counterInBytes, 0, blockForProcess, length, counterInBytes.length);

                byte[] encryptedBlock = BinaryOperations.xor(block, cipherAlgorithm.encryptBlock(blockForProcess));
                System.arraycopy(encryptedBlock, 0, result, startIndex, encryptedBlock.length);
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

//@AllArgsConstructor
//public class CTR implements EncryptionMode, AutoCloseable {
//    private final CipherAlgorithms cipherAlgorithm;
//    private final byte[] IV;
//    private final ExecutorService executorService;
//
//    @Override
//    public byte[] encrypt(byte[] text) {
//        return multiprocessingText(text);
//    }
//
//    @Override
//    public byte[] decrypt(byte[] text) {
//        return multiprocessingText(text);
//    }
//
//    private byte[] multiprocessingText(byte[] text) {
//        int blockLength = cipherAlgorithm.getBlockSize();
//        int countBlocks = (text.length + blockLength - 1) / blockLength; // округление вверх
//        byte[] result = new byte[text.length];
//        List<Future<?>> futures = new ArrayList<>(countBlocks);
//
//        for (int i = 0; i < countBlocks; ++i) {
//            final int index = i;
//
//            futures.add(executorService.submit(() -> {
//                int startIndex = index * blockLength;
//                int remaining = Math.min(blockLength, text.length - startIndex);
//
//                byte[] block = new byte[remaining];
//                System.arraycopy(text, startIndex, block, 0, remaining);
//
//                // Генерация nonce + счетчика для каждого блока
//                byte[] counterBlock = IV.clone();  // Начинаем с IV
//                BigInteger counter = new BigInteger(counterBlock).add(BigInteger.valueOf(index));  // Увеличиваем счетчик
//                byte[] encryptedCounter = cipherAlgorithm.encryptBlock(counter.toByteArray());
//
//                // Шифруем данные, выполняя XOR с зашифрованным счетчиком
//                byte[] encryptedBlock = BinaryOperations.xor(block, encryptedCounter);
//                System.arraycopy(encryptedBlock, 0, result, startIndex, remaining);
//            }));
//        }
//
//        for (var future : futures) {
//            try {
//                future.get();
//            } catch (InterruptedException | ExecutionException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
//
//        return result;
//    }
//
//    @Override
//    public void close() {
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//        }
//    }
//}