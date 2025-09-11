package org.example;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeyVerify;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class CryptoTest {
    Crypto crypto = new Crypto();

    // 确保输出的公钥可被正确读取（getPKString方法、readK方法正常运行）
    @Test
    public void getKeysTest() throws GeneralSecurityException, IOException {
        String publicKey = crypto.getPublicKeysetString();
        System.out.println(publicKey);
        KeysetHandle publicKeysetHandle = crypto.readKeyset(publicKey);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(out));
        String publicKeyRead = out.toString(StandardCharsets.UTF_8);
        Assert.assertEquals(publicKey, publicKeyRead);
    }
    // 确保签名正常运行
    @Test
    public void signTest() throws GeneralSecurityException, IOException {
        String signature = crypto.sign("hello", "123", "abcd");

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        PublicKeyVerify verifier = crypto.publicKeysetHandle.getPrimitive(PublicKeyVerify.class);
        verifier.verify(signatureBytes, "hello|123|abcd".getBytes(StandardCharsets.UTF_8));
    }
    // 确保初次运行时的密钥被正常保存（弃用）
    @Ignore
    public void saveTest() throws GeneralSecurityException, IOException {
        boolean flag = crypto.saveKeysets();
//        Assert.assertFalse(flag);
        Assert.assertTrue(flag);
        String home = System.getProperty("user.home");
        String privateKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_private_key.json";
        String publicKeyFilePath = home + File.separator + ".device_key" + File.separator + "client_public_key.json";
        KeysetHandle privateKeySetHandle = crypto.readKeysetFromFile(new File(privateKeyFilePath), crypto.aeadKeysetHandle);
        KeysetHandle publicKeyHandle = crypto.readKeysetFromFile(new File(publicKeyFilePath), null);
        Assert.assertEquals(crypto.privateKeysetHandle.getKeysetInfo(), privateKeySetHandle.getKeysetInfo());
        Assert.assertEquals(crypto.publicKeysetHandle.getKeysetInfo(), publicKeyHandle.getKeysetInfo());
    }
    // 确保密钥被正确读取（必须先删除所有密钥文件）
    @Ignore
    public void readFileTest() {
        Crypto crypto2 = new Crypto();
//        System.out.println("公钥信息：" + crypto2.publicKeysetHandle.getKeysetInfo());
//        System.out.println("私钥信息：" + crypto2.privateKeysetHandle.getKeysetInfo());
        Assert.assertEquals(crypto.privateKeysetHandle.getKeysetInfo(), crypto2.privateKeysetHandle.getKeysetInfo());
        Assert.assertEquals(crypto.publicKeysetHandle.getKeysetInfo(), crypto2.publicKeysetHandle.getKeysetInfo());
    }
    @Test
    public void verifyTest() throws GeneralSecurityException {
        String signature = "ARPQv7/ooAkpHTSq3v/Kwhe6cRqOI/3ha4fWl+siRQZ6W41uGcWPrv2KRhn+88RxfuuZGd0YcGfWEAjO5zhUkRV7It0E";
        String CPUID = "178BFBFF00A70F52";
        String nonce = "abc";
        String str = CPUID + "|" + nonce;

        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        PublicKeyVerify verifier = crypto.publicKeysetHandle.getPrimitive(PublicKeyVerify.class);
        verifier.verify(signatureBytes, str.getBytes(StandardCharsets.UTF_8));
    }
}
