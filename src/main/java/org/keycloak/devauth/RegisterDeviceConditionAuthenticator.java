package org.keycloak.devauth;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class RegisterDeviceConditionAuthenticator implements ConditionalAuthenticator {
    @Override
    public boolean matchCondition(AuthenticationFlowContext authenticationFlowContext) {
        String registeringDevice = authenticationFlowContext.getAuthenticationSession().getClientNote("registeringDevice");
        return "true".equals(registeringDevice);
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }
}
