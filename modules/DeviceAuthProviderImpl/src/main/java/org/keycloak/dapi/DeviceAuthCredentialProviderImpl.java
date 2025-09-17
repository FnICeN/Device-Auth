package org.keycloak.dapi;

import com.DeviceAuthApi.DeviceAuthCredentialModel;
import com.DeviceAuthApi.DeviceAuthCredentialProvider;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.dapi.Util.CryptoUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class DeviceAuthCredentialProviderImpl implements DeviceAuthCredentialProvider {
    protected KeycloakSession session;

    public DeviceAuthCredentialProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String s) {
        if (!supportsCredentialType(s)) return false;
        return userModel.credentialManager().getStoredCredentialsByTypeStream(s).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) return false;
        if (!(credentialInput.getType().equals(getType()))) return false;
        String challengeResponse = credentialInput.getChallengeResponse();
        if (challengeResponse == null) return false;
        String credentialId = credentialInput.getCredentialId();
        if (credentialId == null || credentialId.isEmpty()) return false;

        // 解析各参数
        String[] parts = challengeResponse.split("\\|\\|");
        if (parts.length != 4) return false;
        String signature = parts[0];
        String visitorId = parts[1];
        String timestamp = parts[2];
        String nonce = parts[3];
        //验证时间戳，防重放
        if (!isTimestampValid(timestamp)) return false;
        System.out.println("timestamp is valid");
        // 获取存储的凭证模型
        CredentialModel cm = userModel.credentialManager().getStoredCredentialById(credentialId);
        DeviceAuthCredentialModel dacm = getCredentialFromModel(cm);
        // 验签（只要CPUID对了、时间戳、nonce未被改动就能通过）
        try {
            String publicKeyJson = dacm.getDeviceName().getPublicKeyJson();
            CryptoUtil cryptoUtil = new CryptoUtil();
            cryptoUtil.verifySign(signature, dacm.getDeviceData().getCpuId(), timestamp, nonce, publicKeyJson);
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
        // 验证浏览器指纹
        boolean visitorIdFlag = dacm.getDeviceData().getVisitorId().equals(visitorId);

        return visitorIdFlag;
    }

    @Override
    public String getType() {
        return DeviceAuthCredentialModel.TYPE;
    }

    // 新注册凭证时会被调用
    @Override
    public CredentialModel createCredential(RealmModel realmModel, UserModel userModel, DeviceAuthCredentialModel deviceAuthCredentialModel) {
        if (deviceAuthCredentialModel.getCreatedDate() == null)
            deviceAuthCredentialModel.setCreatedDate(Time.currentTimeMillis());
        return userModel.credentialManager().createStoredCredential(deviceAuthCredentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realmModel, UserModel userModel, String s) {
        return userModel.credentialManager().removeStoredCredentialById(s);
    }

    @Override
    public DeviceAuthCredentialModel getCredentialFromModel(CredentialModel credentialModel) {
        DeviceAuthCredentialModel dacm = DeviceAuthCredentialModel.createFromCredentialModel(credentialModel);
        return dacm;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext credentialTypeMetadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName(DeviceAuthCredentialProviderFactory.PROVIDER_ID)
                .helpText("device-authenticate")
                .createAction("device-auth-authenticator")
                .removeable(false)
                .build(session);
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return getType().equals(type);
    }
    private boolean isTimestampValid(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            long oneMinuteMillis = 60 * 1000;
            // 判断 timestamp 是否在 [now - 1min, now + 1min] 之间
            return Math.abs(now - timestamp) <= oneMinuteMillis;
        } catch (NumberFormatException e) {
            // 非法时间戳
            return false;
        }
    }
}
