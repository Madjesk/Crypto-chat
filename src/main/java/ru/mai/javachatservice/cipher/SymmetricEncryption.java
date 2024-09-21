package ru.mai.javachatservice.cipher;

import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.cipher.cipher_impl.mode.CBC.CBC;
import ru.mai.javachatservice.cipher.cipher_impl.mode.CTR.CTR;
import ru.mai.javachatservice.cipher.cipher_impl.mode.ECB.ECB;
import ru.mai.javachatservice.cipher.cipher_impl.mode.CFB.CFB;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_impl.mode.OFB.OFB;
import ru.mai.javachatservice.cipher.cipher_impl.mode.PCBC.PCBC;
import ru.mai.javachatservice.cipher.cipher_impl.mode.RandomDelta.RandomDelta;
import ru.mai.javachatservice.cipher.cipher_impl.padding.Padding;
import ru.mai.javachatservice.cipher.cipher_impl.padding.impl.PKCS7;
import ru.mai.javachatservice.cipher.cipher_impl.padding.impl.ANSI_X923;
import ru.mai.javachatservice.cipher.cipher_impl.padding.impl.ISO_10126;
import ru.mai.javachatservice.cipher.cipher_impl.padding.impl.Zeros;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_impl.FileThreadCipherImpl;
import ru.mai.javachatservice.cipher.cipher_thread.file.file_impl.FileThreadTaskCipherImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.*;

@Slf4j
public class SymmetricEncryption implements AutoCloseable {

    public enum EncryptionModes {
        ECB,
        CBC,
        PCBC,
        CFB,
        OFB,
        CTR,
        RANDOM_DELTA
    }

    public enum EncryptionAlgorithm {
        RC6,
        SERPENT
    }

    public enum PaddingMode {
        ZEROS,
        ANSI_X923,
        PKCS7,
        ISO_10126
    }

    private final ExecutorService executorService;
    private final EncryptionMode encryptionMode;
    private final Padding padding;
    private final CipherAlgorithms cipherAlgorithm;
    private final byte[] initializationVector_IV;

    public SymmetricEncryption(EncryptionModes encryptionMode, PaddingMode paddingMode, CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV) {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        this.encryptionMode = switch (encryptionMode) {
            case ECB -> new ECB(cipherAlgorithm, executorService);
            case CBC -> new CBC(cipherAlgorithm, initializationVector_IV, executorService);
            case PCBC -> new PCBC(cipherAlgorithm, initializationVector_IV, executorService);
            case CFB -> new CFB(cipherAlgorithm, initializationVector_IV, executorService);
            case OFB -> new OFB(cipherAlgorithm, initializationVector_IV);
            case CTR -> new CTR(cipherAlgorithm, initializationVector_IV, executorService);
            case RANDOM_DELTA -> new RandomDelta(cipherAlgorithm, initializationVector_IV, executorService);
        };

        this.padding = switch(paddingMode) {
            case ZEROS -> new Zeros();
            case ANSI_X923 -> new ANSI_X923();
            case PKCS7 -> new PKCS7();
            case ISO_10126 -> new ISO_10126();
        };

        this.cipherAlgorithm = cipherAlgorithm;
        this.initializationVector_IV = initializationVector_IV;
    }


    public byte[] encrypt(byte[] textToEncrypt) throws ExecutionException, InterruptedException {
        log.info("Starting encrypt byte text");
        try {
            return encryptionMode.encrypt(padding.addPadding(textToEncrypt, cipherAlgorithm.getBlockSize()));
        } catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return new byte[0];
    }

    public byte[] decrypt(byte[] textToDecrypt) throws ExecutionException, InterruptedException {
        log.info("Starting decrypt byte text");
        try {
            return padding.removePadding(encryptionMode.decrypt(textToDecrypt));
        }

        catch (Exception ex) {
            log.error(ex.getMessage());
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return new byte[0];
    }



    private String addPostfixToFileName(String pathToInputFile, String postfix) {
        log.info("Starting add Postfix to file");
        int dotIndex = pathToInputFile.lastIndexOf('.');
        String baseName = pathToInputFile.substring(0, dotIndex);
        String extension = pathToInputFile.substring(dotIndex);
        return baseName + postfix + extension;
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }
    }
}