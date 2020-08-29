package net.ccbluex.liquidbounce.utils;

import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class BarrageFile {
    private static final byte[] ADDITIONAL_SIGNATURE = Base64.getDecoder().decode("SAldciJNaza2lFm1wXg31ptIcKiDBmH4i24c1J0YioB55fmDuDW2V5iPOzL7ivfh1u0QnXVMhVSJUbQEgOVxq4vq4C0r");

    public static byte[] read(InputStream inputStream, PublicKeyValidator keyValidator) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        int publicKeyLength = dataInputStream.readUnsignedShort();

        if (publicKeyLength <= 0)
            throw new IllegalArgumentException("Length has to be at least 1");

        byte[] publicKeyData = new byte[publicKeyLength];

        dataInputStream.readFully(publicKeyData);

        if (keyValidator != null && !keyValidator.verifyHash(publicKeyData))
            throw new IllegalArgumentException("The certificate was rejected by the authority");

        int headerLength = dataInputStream.readUnsignedShort();

        if (headerLength <= 0)
            throw new IllegalArgumentException("Length has to be at least 1");

        byte[] encryptedHeaderData = new byte[headerLength];

        dataInputStream.readFully(encryptedHeaderData);

        int encryptedDataLength = dataInputStream.readInt();

        if (encryptedDataLength <= 0)
            throw new IllegalArgumentException("Length has to be at least 1");

        byte[] encryptedData = new byte[encryptedDataLength];

        dataInputStream.readFully(encryptedData);

        // Decrypt header

        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        X509EncodedKeySpec publicKey = new X509EncodedKeySpec(publicKeyData);

        rsa.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(publicKey));

        byte[] decryptedHeader = rsa.doFinal(encryptedHeaderData);

        MessageDigest sha = MessageDigest.getInstance("SHA-512");
        BarrageFileHeader headerStruct = new BarrageFileHeader(new ByteArrayInputStream(decryptedHeader));

        if (!Arrays.equals(headerStruct.additionalSignature, ADDITIONAL_SIGNATURE))
            throw new IllegalArgumentException("Additional signature is invalid");

        byte[] publicKeyHash = sha.digest(publicKeyData);

        if (!Arrays.equals(publicKeyHash, headerStruct.publicKeyHash))
            throw new IllegalArgumentException("Public key is invalid");

        byte[] data;

        if (!isZero(headerStruct.getAesKey()) || !isZero(headerStruct.getIV())) {
            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(headerStruct.getAesKey(), "AES"), new IvParameterSpec(headerStruct.getIV()));

            data = aes.doFinal(encryptedData);
        } else {
            data = encryptedData;
        }

        if (!Arrays.equals(sha.digest(data), headerStruct.dataHash))
            throw new IllegalArgumentException("Data is invalid");

        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte aByte : bytes) {
            for (int j = 1; j >= 0; j--) {
                int shift = (j * 4);
                int b = (aByte & (0xF << shift)) >> shift;

                if (b < 10)
                    sb.append((char) ('0' + b));
                else
                    sb.append((char) ('a' + (b - 10)));
            }
        }

        return sb.toString();
    }

    @NotNull
    public static byte[] hexStringToByteArray(String text) {
        byte[] bytes = new byte[(text.length() | 1) / 2];

        char[] chars = text.toLowerCase().toCharArray();

        int index = 0;
        boolean shift = true;

        for (char c : chars) {
            int val;

            if (c >= '0' && c <= '9')
                val = c - '0';
            else if (c >= 'a' && c <= 'f')
                val = 10 + c - 'a';
            else
                throw new IllegalArgumentException("Invalid character hex string ('" + c + "')");

            bytes[index] |= val << (shift ? 4 : 0);

            shift = !shift;

            if (shift)
                index++;
        }

        return bytes;
    }

    private static boolean isZero(byte[] aesKey) {
        for (byte b : aesKey) {
            if (b != 0)
                return false;
        }

        return true;
    }

    public interface PublicKeyValidator {
        boolean verifyHash(byte[] sha512);
    }

    public static class BarrageFileHeader {
        private final byte[] aesKey = new byte[32]; // AES 256 -> 32 byte key
        private final byte[] iv = new byte[16];
        private final byte[] publicKeyHash = new byte[64];
        private final byte[] dataHash = new byte[64];
        private final byte[] additionalSignature = new byte[69];

        public BarrageFileHeader(InputStream byteArrayInputStream) throws IOException {
            DataInputStream dataIn = new DataInputStream(byteArrayInputStream);

            dataIn.readFully(aesKey);
            dataIn.readFully(iv);
            dataIn.readFully(publicKeyHash);
            dataIn.readFully(dataHash);
            dataIn.readFully(additionalSignature);
        }

        public static byte[] generate(byte[] publicKey, byte[] data, Cipher aesCipherToInit, boolean aes) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, InvalidKeyException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);

            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

            SecureRandom secureRandom = new SecureRandom();

            byte[] aesKey = new byte[32];

            byte[] iv = new byte[16];

            if (aes) {
                do {
                    secureRandom.nextBytes(aesKey);
                    secureRandom.nextBytes(iv);
                } while (isZero(aesKey) && isZero(iv));
            }

            aesCipherToInit.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));

            byte[] publicKeyHash = sha512.digest(publicKey);
            byte[] dataHash = sha512.digest(data);

            if (publicKeyHash.length != 64)
                throw new IllegalStateException("SHA-512 returned an " + publicKeyHash.length + " long hash (wtf?)");
            if (dataHash.length != 64)
                throw new IllegalStateException("SHA-512 returned an " + dataHash.length + " long hash (wtf?)");

            baos.write(aesKey);
            baos.write(iv);
            baos.write(publicKeyHash);
            baos.write(dataHash);
            baos.write(ADDITIONAL_SIGNATURE);

            return baos.toByteArray();
        }

        public byte[] getAesKey() {
            return aesKey;
        }

        public byte[] getIV() {
            return iv;
        }

        public byte[] getPublicKeyHash() {
            return publicKeyHash;
        }

        public byte[] getDataHash() {
            return dataHash;
        }

        public byte[] getAdditionalSignature() {
            return additionalSignature;
        }
    }
}
