package ru.mai.javachatservice.cipher.cipher_impl.mode.RandomDelta;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.utils.BinaryOperations;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ExecutionException;

public class RandomDelta implements EncryptionMode, AutoCloseable {
    private final CipherAlgorithms cipherAlgorithm;
    private final byte[] IV;
    private final ExecutorService executorService;
    private final ThreadLocal<byte[]> threadLocalBuffer;
    private final BigInteger delta;

    public RandomDelta(CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV, ExecutorService executorService) {
        this.cipherAlgorithm = cipherAlgorithm;
        this.IV = initializationVector_IV;
        this.executorService = executorService;
        this.threadLocalBuffer = ThreadLocal.withInitial(() -> new byte[cipherAlgorithm.getBlockSize()]);
        delta = new BigInteger(Arrays.copyOf(IV, cipherAlgorithm.getBlockSize() / 2));
    }

    @Override
    public byte[] encrypt(byte[] text) {
        return multiprocessingText(text, true);
    }

    @Override
    public byte[] decrypt(byte[] text) {
        return multiprocessingText(text, false);
    }

    private byte[] multiprocessingText(byte[] data, boolean encryptOrDecrypt) {
        int blockLength = cipherAlgorithm.getBlockSize();
        byte[] result = new byte[data.length];
        BigInteger initialStart = new BigInteger(IV);
        int countBlocks = data.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(countBlocks);

        for (int i = 0; i < countBlocks; ++i) {
            final int index = i;

            futures.add(executorService.submit(() -> {
                BigInteger initial = initialStart.add(delta.multiply(BigInteger.valueOf(index)));
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                // byte[] block = threadLocalBuffer.get();
                System.arraycopy(data, startIndex, block, 0, blockLength);

                byte[] encryptedOrDecryptedBlock = encryptOrDecrypt ?
                        cipherAlgorithm.encryptBlock(BinaryOperations.xor(initial.toByteArray(), block)) :
                        BinaryOperations.xor(cipherAlgorithm.decryptBlock(block), initial.toByteArray());
                System.arraycopy(encryptedOrDecryptedBlock, 0, result, startIndex, encryptedOrDecryptedBlock.length);
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
