package org.keycloak.dapi.Util;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.signature.SignatureConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class CryptoUtil {
    KeysetHandle publicKeysetHandle;
    public CryptoUtil() throws GeneralSecurityException {
        SignatureConfig.register();
    }

    public void verifySign(String signature, String cpuid, String timestamp, String nonce, String publicKeyStr) throws GeneralSecurityException, IOException {
        System.out.println("verifying...");
        // 拼接真实信息
        String originMsg = cpuid + "|" + timestamp + "|" + nonce;
        // 读取公钥
        publicKeysetHandle = readKeyset(publicKeyStr);
        // 进行验签
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        PublicKeyVerify verifier = publicKeysetHandle.getPrimitive(PublicKeyVerify.class);
        verifier.verify(signatureBytes, originMsg.getBytes(StandardCharsets.UTF_8));
        System.out.println("signature verify pass!");
    }

    public KeysetHandle readKeyset(String keyJson) throws GeneralSecurityException, IOException {
        KeysetHandle publicKeysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(keyJson));
        return publicKeysetHandle;
    }
}
