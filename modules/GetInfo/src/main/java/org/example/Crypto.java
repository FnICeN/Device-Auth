package org.example;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.signature.SignatureConfig;
import com.google.crypto.tink.signature.SignatureKeyTemplates;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class Crypto {
    KeysetHandle privateKeysetHandle;
    KeysetHandle publicKeysetHandle;
    KeysetHandle aeadKeysetHandle;
    String privateKeyFilePath, publicKeyFilePath;
    public Crypto() {
        try {
            SignatureConfig.register();
            // 获取密钥文件目录
            String home = System.getProperty("user.home");
            privateKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_private_key.json";
            publicKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_public_key.json";
            // 新建或读取密钥文件
            if (existKeysets()) {  // 应新建
                privateKeysetHandle = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519);
                publicKeysetHandle = privateKeysetHandle.getPublicKeysetHandle();
                saveKeysets();
                System.out.println("公钥信息：" + publicKeysetHandle.getKeysetInfo());
                System.out.println("私钥信息：" + privateKeysetHandle.getKeysetInfo());
            } else {  // 应读取
                AeadConfig.register();
                String aeadKeyFilePath = home + File.separator + ".device_key" + File.separator + "aead.json";
                aeadKeysetHandle = readKeysetFromFile(new File(aeadKeyFilePath), null);
                privateKeysetHandle = readKeysetFromFile(new File(privateKeyFilePath), aeadKeysetHandle);
                publicKeysetHandle = readKeysetFromFile(new File(publicKeyFilePath), null);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String getPublicKeysetString() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(out));
        String publicKeyJson = out.toString(StandardCharsets.UTF_8);
        return publicKeyJson;
    }
    public KeysetHandle readKeyset(String keyJson) throws GeneralSecurityException, IOException {
        KeysetHandle publicKeysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(keyJson));
        return publicKeysetHandle;
    }
    public String sign(String msg, String timestamp, String nonce) {
        try {
            SignatureConfig.register();
            String inf = msg + "|" + timestamp + "|" + nonce;
            byte[] nonceBytes = inf.getBytes(StandardCharsets.UTF_8);
            PublicKeySign pks = privateKeysetHandle.getPrimitive(PublicKeySign.class);
            byte[] signatureBytes = pks.sign(nonceBytes);
            String signature = Base64.getEncoder().encodeToString(signatureBytes);  // 在此处将签名结果编码为Base64
            return signature;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean saveKeysets() throws GeneralSecurityException, IOException {
        AeadConfig.register();
        String home = System.getProperty("user.home");
        String privateKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_private_key.json";
        String publicKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_public_key.json";
        String aeadKeyFilePath = home + File.separator + ".device_key" + File.separator + "aead.json";
        File keyFile = new File(privateKeyFilePath);
        if (keyFile.exists()) {
            System.out.println("已有私钥文件，不保存新私钥");
            return false;
        }
        File parentDir = keyFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        aeadKeysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
        CleartextKeysetHandle.write(aeadKeysetHandle, JsonKeysetWriter.withFile(new File(aeadKeyFilePath)));
        System.out.println("AEAD保存至：" + aeadKeyFilePath);
        Aead aead = aeadKeysetHandle.getPrimitive(Aead.class);
        // 保存私钥
        privateKeysetHandle.write(JsonKeysetWriter.withFile(new File(privateKeyFilePath)), aead);
        System.out.println("私钥保存至：" + privateKeyFilePath);
        // 保存公钥
        CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withFile(new File(publicKeyFilePath)));
        System.out.println("公钥保存至：" + publicKeyFilePath);

        return true;

    }
    public KeysetHandle readKeysetFromFile(File keysetFile, @Nullable KeysetHandle aeadHandle) throws GeneralSecurityException, IOException {
        KeysetHandle resKeyHandle;
        if (aeadHandle == null) {
            resKeyHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(keysetFile));
            return resKeyHandle;
        }
        resKeyHandle = KeysetHandle.read(JsonKeysetReader.withFile(keysetFile), aeadHandle.getPrimitive(Aead.class));
        return resKeyHandle;
    }
    private boolean existKeysets() {
        File keyFile = new File(this.privateKeyFilePath);
        if (keyFile.exists()) {
            System.out.println("已有私钥文件，不保存新私钥");
            return false;
        }
        return true;
    }
}
