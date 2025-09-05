package com.DeviceAuthApi;

import org.keycloak.credential.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public interface DeviceAuthCredentialProvider extends CredentialProvider<DeviceAuthCredentialModel>, CredentialInputValidator {
    @Override
    boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String s);

    @Override
    boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput);

    @Override
    default void close() {
        CredentialProvider.super.close();
    }

    @Override
    String getType();

    @Override
    CredentialModel createCredential(RealmModel realmModel, UserModel userModel, DeviceAuthCredentialModel deviceAuthCredentialModel);

    @Override
    boolean deleteCredential(RealmModel realmModel, UserModel userModel, String s);

    @Override
    DeviceAuthCredentialModel getCredentialFromModel(CredentialModel credentialModel);

    @Override
    default DeviceAuthCredentialModel getDefaultCredential(KeycloakSession session, RealmModel realm, UserModel user) {
        return CredentialProvider.super.getDefaultCredential(session, realm, user);
    }

    @Override
    CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext credentialTypeMetadataContext);

    @Override
    default CredentialMetadata getCredentialMetadata(DeviceAuthCredentialModel credentialModel, CredentialTypeMetadata credentialTypeMetadata) {
        return CredentialProvider.super.getCredentialMetadata(credentialModel, credentialTypeMetadata);
    }

    @Override
    default boolean supportsCredentialType(CredentialModel credential) {
        return CredentialProvider.super.supportsCredentialType(credential);
    }

    @Override
    default boolean supportsCredentialType(String type) {
        return CredentialProvider.super.supportsCredentialType(type);
    }
}
