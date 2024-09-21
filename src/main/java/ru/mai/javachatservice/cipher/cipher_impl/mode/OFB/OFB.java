package ru.mai.javachatservice.cipher.cipher_impl.mode.OFB;

import lombok.AllArgsConstructor;
import ru.mai.javachatservice.cipher.cipher_impl.mode.EncryptionMode;
import ru.mai.javachatservice.cipher.cipher_interface.CipherAlgorithms;
import ru.mai.javachatservice.cipher.utils.BinaryOperations;
public class OFB implements EncryptionMode {
    private final CipherAlgorithms cipherAlgorithm;
    private final byte[] IV;
    private final ThreadLocal<byte[]> threadLocalBuffer;

    public OFB(CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV) {
        this.cipherAlgorithm = cipherAlgorithm;
        this.IV = initializationVector_IV;
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
        byte[] previousBlock = IV;
        int length = text.length / blockLength;

        for (int i = 0; i < length; ++i) {
            int startIndex = i * blockLength;
            byte[] block = new byte[blockLength];
            // byte[] block = threadLocalBuffer.get();
            System.arraycopy(text, startIndex, block, 0, blockLength);

            byte[] encryptedPart = cipherAlgorithm.encryptBlock(previousBlock);
            byte[] encryptedOrDecryptedBlock = BinaryOperations.xor(block, encryptedPart);

            System.arraycopy(encryptedOrDecryptedBlock, 0, result, startIndex, encryptedOrDecryptedBlock.length);
            previousBlock = encryptedPart;
        }

        return result;
    }
}
//public class OFB implements EncryptionMode {
//    private final CipherAlgorithms cipherAlgorithm;
//    private final byte[] IV;
//
//    public OFB(CipherAlgorithms cipherAlgorithm, byte[] initializationVector_IV) {
//        this.cipherAlgorithm = cipherAlgorithm;
//        this.IV = initializationVector_IV;
//    }
//
//    @Override
//    public byte[] encrypt(byte[] text) {
//        return processText(text);
//    }
//
//    @Override
//    public byte[] decrypt(byte[] text) {
//        return processText(text);  // В OFB шифрование и дешифрование одинаковы
//    }
//
//    private byte[] processText(byte[] text) {
//        int blockLength = cipherAlgorithm.getBlockSize();
//        byte[] result = new byte[text.length];
//        byte[] previousBlock = IV.clone();  // Клонирование IV для работы с блоками
//
//        // Шифрование полных блоков
//        int fullBlocksCount = text.length / blockLength;
//        for (int i = 0; i < fullBlocksCount; ++i) {
//            int startIndex = i * blockLength;
//            byte[] block = new byte[blockLength];
//            System.arraycopy(text, startIndex, block, 0, blockLength);
//
//            // Шифруем IV или предыдущий блок
//            byte[] encryptedPart = cipherAlgorithm.encryptBlock(previousBlock);
//            // XOR для получения зашифрованного или расшифрованного блока
//            byte[] encryptedOrDecryptedBlock = BinaryOperations.xor(block, encryptedPart);
//            System.arraycopy(encryptedOrDecryptedBlock, 0, result, startIndex, blockLength);
//
//            // Обновляем предыдущий блок для следующего шага
//            previousBlock = encryptedPart;
//        }
//
//        // Обработка последнего неполного блока, если такой есть
//        int remainingBytes = text.length % blockLength;
//        if (remainingBytes > 0) {
//            int startIndex = fullBlocksCount * blockLength;
//            byte[] lastBlock = new byte[remainingBytes];
//            System.arraycopy(text, startIndex, lastBlock, 0, remainingBytes);
//
//            // Шифруем предыдущий блок для получения потока данных
//            byte[] encryptedPart = cipherAlgorithm.encryptBlock(previousBlock);
//            byte[] truncatedEncryptedPart = new byte[remainingBytes];
//            System.arraycopy(encryptedPart, 0, truncatedEncryptedPart, 0, remainingBytes);
//
//            // XOR для последнего неполного блока
//            byte[] encryptedOrDecryptedBlock = BinaryOperations.xor(lastBlock, truncatedEncryptedPart);
//            System.arraycopy(encryptedOrDecryptedBlock, 0, result, startIndex, remainingBytes);
//        }
//
//        return result;
//    }
//}
