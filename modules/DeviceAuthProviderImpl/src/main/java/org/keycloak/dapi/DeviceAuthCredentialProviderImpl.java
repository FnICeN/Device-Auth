package org.keycloak.dapi;

import com.DeviceAuthApi.DeviceAuthCredentialModel;
import com.DeviceAuthApi.DeviceAuthCredentialProvider;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

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

        CredentialModel cm = userModel.credentialManager().getStoredCredentialById(credentialId);
        DeviceAuthCredentialModel dacm = getCredentialFromModel(cm);

        String[] parts = challengeResponse.split("\\|\\|", 2);
        boolean cpuIdFlag = dacm.getDeviceData().getCpuId().equals(parts[0]);
        boolean visitorIdFlag = dacm.getDeviceData().getVisitorId().equals(parts.length > 1 ? parts[1] : "");

        return cpuIdFlag && visitorIdFlag;
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
}
