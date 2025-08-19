package org.keycloak.devauth;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DeviceAuthRequiredActionFactory implements RequiredActionFactory {
    private static final DeviceAuthRequiredAction SINGLETON = new DeviceAuthRequiredAction();
    @Override
    public String getDisplayText() {
        return "Device Authentication";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession keycloakSession) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return DeviceAuthRequiredAction.PROVIDER_ID;
    }
}
