package org.keycloak.dapi;

import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

public class DeviceAuthCredentialProviderFactory implements CredentialProviderFactory<DeviceAuthCredentialProviderImpl> {
    public static final String PROVIDER_ID = "device-auth";

    @Override
    public CredentialProvider create(KeycloakSession keycloakSession) {
        return new DeviceAuthCredentialProviderImpl(keycloakSession);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
