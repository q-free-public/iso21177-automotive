package com.qfree.its.iso21177poc.common.geoflow;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Base64;

public class EncryptedKeyInfo {
    private byte[] salt;
    private byte[] cipherText;
    private byte[] iv;

    public static EncryptedKeyInfo readFromFile(Context context, String filename) throws Exception {
        File privateKeyDir = new File(context.getFilesDir(), EncryptionKeyUtils.PRIVATE_KEY_DIRECTORY);
        if (!privateKeyDir.exists()){
            privateKeyDir.mkdir();
        }
        File privateKeyFile = new File(privateKeyDir, filename + ".json");
        EncryptedKeyInfo encryptedKeyInfo;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(privateKeyFile))) {
            Gson gson = new Gson();
            encryptedKeyInfo = gson.fromJson(bufferedReader, EncryptedKeyInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("No private key found");
        }
        return encryptedKeyInfo;
    }

    public void writeToFile(Context context, String filename) throws Exception {
        File privateKeyDir = new File(context.getFilesDir(), EncryptionKeyUtils.PRIVATE_KEY_DIRECTORY);
        if (!privateKeyDir.exists()){
            privateKeyDir.mkdir();
        }
        File privateKeyFile = new File(privateKeyDir, filename + ".json");
        try (PrintWriter printWriter = new PrintWriter(privateKeyFile)) {
            Gson gson = new Gson();
            printWriter.println(gson.toJson(this));
        }
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] cipherText) {
        this.cipherText = cipherText;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public void setSaltFromBase64Str(String saltBase64) {
        this.salt = Base64.getDecoder().decode(saltBase64);
    }

    public void setCipherTextFromBase64Str(String cipherTextBase64) {
        this.cipherText = Base64.getDecoder().decode(cipherTextBase64);
    }

    public void setIvFromBase64Str(String ivBase64) {
        this.iv = Base64.getDecoder().decode(ivBase64);

    }

    public String getSaltBase64Str() {
        return Base64.getEncoder().withoutPadding().encodeToString(salt);
    }

    public String getCipherTextBase64Str() {
        return Base64.getEncoder().withoutPadding().encodeToString(cipherText);
    }

    public String getIvBase64Str() {
        return Base64.getEncoder().withoutPadding().encodeToString(iv);
    }

}
