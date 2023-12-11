package com.qfree.its.iso21177poc.common.geoflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecord;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItem;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordSecured;
import com.qfree.geoflow.toll.api.GeoFlowWsApiRegistrationRequest;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionKeyUtils {
    public static final String PRIVATE_KEY_DIRECTORY = "keys";

    private static final String TAG = EncryptionKeyUtils.class.getSimpleName();

    //For encryption
    public static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance("RSA");
        keyPair.initialize(2048);
        return keyPair.generateKeyPair();
    }

    public static PrivateKey recoverPrivateKeyFromByteArray(byte[] privateKeyBytes) throws Exception {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = factory.generatePrivate(pkcs8EncodedKeySpec);
        return privateKey;
    }

    public static PublicKey recoverPublicKeyFromByteArray(byte[] publicSigningKeyArr) throws Exception {
        X509EncodedKeySpec keySpecPKCS8 = new X509EncodedKeySpec(publicSigningKeyArr);
        KeyFactory kf2 = KeyFactory.getInstance("EC");
        PublicKey publicKey = kf2.generatePublic(keySpecPKCS8);
        return publicKey;
    }

    //For signing
    public static KeyPair generateECKeyPair() throws Exception {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance("EC");
        keyPair.initialize(new ECGenParameterSpec("secp384r1"));
        return keyPair.generateKeyPair();
    }

    public static PrivateKey byteArrayToECPrivateKey(byte[] privateKeyForSigning) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyForSigning);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey byteArrayToRSAPublicKey(byte[] publicKeyForEncryption) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyForEncryption);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static void storePrivateKeyEncrypted(Context context, String filename, String pinCode, PrivateKey privateKey) throws Exception {
        PasswordKeyInfo passwordKeyInfo = deriveKeyFromPasswordFirstTime(pinCode);
        EncryptInfo encryptedPrivateKey = encrypt(passwordKeyInfo, privateKey.getEncoded());
        EncryptedKeyInfo encryptedKeyInfo = new EncryptedKeyInfo();
        encryptedKeyInfo.setSalt(passwordKeyInfo.salt);
        encryptedKeyInfo.setCipherText(encryptedPrivateKey.cipherText);
        encryptedKeyInfo.setIv(encryptedPrivateKey.iv);
        //Save to file
        encryptedKeyInfo.writeToFile(context, filename);
    }

//    public static PrivateKey getPrivateKeyDecrypted(Context context, String filename, String pinCode) throws Exception {
//        PrivateKey privateKeyEncoded = null;
//        try {
//            EncryptedKeyInfo encryptedKeyInfo = EncryptedKeyInfo.readFromFile(context, filename);
//            if (encryptedKeyInfo != null) {
//                PasswordKeyInfo passwordKeyInfoDecrypt = deriveKeyFromPasswordSecondTime(pinCode, encryptedKeyInfo.getSalt());
//                byte[] decrypted = decrypt(context, passwordKeyInfoDecrypt, encryptedKeyInfo.getIv(), encryptedKeyInfo.getCipherText());
//                privateKeyEncoded = recoverPrivateKeyFromByteArray(decrypted);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new Exception(e.getMessage());
//        }
//        return privateKeyEncoded;
//    }

    private static PasswordKeyInfo deriveKeyFromPasswordFirstTime(String password) throws Exception {
        System.out.println("Test key derivation (no salt - first time)");
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[32];
        rnd.nextBytes(salt);
        return deriveKeyFromPasswordSecondTime(password, salt);
    }

    private static PasswordKeyInfo deriveKeyFromPasswordSecondTime(String password, byte[] salt) throws Exception {
        System.out.println("Test key derivation (known salt)");
        char[] chars = new char[password.length()];
        password.getChars(0, password.length(), chars, 0);
        System.out.println("Salt: " + bin2hex(salt));
        byte[] key = jcePKCS5Scheme2(chars, salt);
        System.out.println("Key: " + bin2hex(key));
        PasswordKeyInfo keyInfo = new PasswordKeyInfo();
        keyInfo.key = key;
        keyInfo.salt = salt;
        return keyInfo;
    }

    /**
     * Calculate a derived key using PBKDF2 based on SHA-256.
     *
     * @param password the password input.
     * @param salt     the salt parameter.
     * @return the derived key.
     */
    public static byte[] jcePKCS5Scheme2(char[] password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory fact = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA256");
        final int iterationCount = 1;
        final int keyLength = 256;
        if (salt.length * 8 != keyLength)
            throw new GeneralSecurityException("salt must be " + keyLength + " bits.");
        return fact.generateSecret(new PBEKeySpec(password, salt, iterationCount, keyLength)).getEncoded();
    }

    private static String bin2hex(byte[] arr) {
        String s = "";
        for (byte b : arr) {
            s += String.format("%02x ", b);
        }
        return s;
    }

    private static EncryptInfo encrypt(PasswordKeyInfo keyInfo, byte[] input) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyInfo.key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        System.out.println("input : " + bin2hex(input));
        SecureRandom rnd = new SecureRandom();
        byte[] iv = new byte[16];
        rnd.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] output = cipher.doFinal(input);
        EncryptInfo encryptedInfo = new EncryptInfo();
        encryptedInfo.cipherText = output;
        encryptedInfo.iv = iv;
        return encryptedInfo;
    }

    private static byte[] decrypt(Context context, PasswordKeyInfo keyInfo, byte[] iv, byte[] secret) throws Exception {
        try {
            SecretKeySpec key = new SecretKeySpec(keyInfo.key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        System.out.println("secret: " + bin2hex(secret));
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] finalOutput = new byte[cipher.getOutputSize(secret.length)];
            int len = cipher.update(secret, 0, secret.length, finalOutput, 0);
            len += cipher.doFinal(finalOutput, len);
            finalOutput = Arrays.copyOfRange(finalOutput, 0, len);
//        System.out.println("decrypted: " + bin2hex(finalOutput));
            return finalOutput;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("wrong pin");
        }
    }

//    public static void saveAccessTokenAsPreference(SharedPreferences sharedPreferences, byte[] accessToken) {
//        sharedPreferences.edit().putString(PreferenceKey.ACCESS_TOKEN, Arrays.toString(accessToken)).apply();
//    }

//    public static byte[] retrieveAccessTokenFromPreference(SharedPreferences sharedPreferences) {
//        String accessTokenStr = sharedPreferences.getString(PreferenceKey.ACCESS_TOKEN, null);
//        if (accessTokenStr != null) {
//            String[] split = accessTokenStr.substring(1, accessTokenStr.length() - 1).split(", ");
//            byte[] accessToken = new byte[split.length];
//            for (int i = 0; i < split.length; i++) {
//                accessToken[i] = Byte.parseByte(split[i]);
//            }
//            return accessToken;
//        }
//        return null;
//    }

//    public static void clearAccessTokenFromPreference(SharedPreferences sharedPreferences) {
//        sharedPreferences.edit().remove(PreferenceKey.ACCESS_TOKEN).apply();
//    }

    static class PasswordKeyInfo {
        byte[] key;
        byte[] salt;
    }

    static class EncryptInfo {
        byte[] cipherText;
        byte[] iv;
    }
}
