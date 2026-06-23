package com.cl_labs;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes();

    public static byte[] encrypt(byte[] plainText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(byte[] cipherText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(cipherText);
    }
}