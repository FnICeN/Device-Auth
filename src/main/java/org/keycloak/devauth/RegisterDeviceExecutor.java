package org.keycloak.devauth;

import com.DeviceAuthApi.DeviceAuthConstants;
import com.DeviceAuthApi.DeviceAuthCredentialModel;
import com.DeviceAuthApi.DeviceAuthCredentialProvider;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class RegisterDeviceExecutor implements Authenticator {
    private Logger logger = Logger.getLogger(RegisterDeviceExecutor.class);
    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        logger.info("进入Registering device executor");
        if (isRegisterDevice(authenticationFlowContext)) {
            logger.info("默认前序认证成功，注册器保存设备信息...");
            String newName = authenticationFlowContext.getAuthenticationSession().getClientNote("newName");
            String cpuid = authenticationFlowContext.getAuthenticationSession().getClientNote("cpuid");
            String visitorId = authenticationFlowContext.getAuthenticationSession().getClientNote("visitorId");
            String publicKeyJson = authenticationFlowContext.getAuthenticationSession().getClientNote("publicKeyJson");
            logger.info("newName: " + newName);
            logger.info("cpuid: " + cpuid);
            logger.info("visitorId: " + visitorId);
            logger.info("publicKeyJson: " + publicKeyJson);
            DeviceAuthCredentialProvider dacp = (DeviceAuthCredentialProvider) authenticationFlowContext.getSession().getProvider(CredentialProvider.class, DeviceAuthConstants.credentialProviderFactoryID);
            dacp.createCredential(authenticationFlowContext.getRealm(), authenticationFlowContext.getUser(), DeviceAuthCredentialModel.createDeviceAuth(newName, publicKeyJson, cpuid, visitorId));
            // TODO：确保设备名称不相同，否则会导致系统错误
            logger.info("成功保存新设备信息");
        } else {
            logger.info("并非注册");
        }
        authenticationFlowContext.success();
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }
    protected boolean isRegisterDevice(AuthenticationFlowContext context) {
        String registeringDevice = context.getAuthenticationSession().getClientNote("registeringDevice");
        return "true".equals(registeringDevice);
    }
}
