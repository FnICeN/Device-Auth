package org.keycloak.devauth;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class DeviceAuthAuthenticator implements Authenticator, CredentialValidator<DeviceAuthCredentialProvider> {
    private Logger logger = Logger.getLogger(DeviceAuthAuthenticator.class);
    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        logger.info("进入认证，显示页面...");
        Response challenge = authenticationFlowContext.form().createForm("DeviceInfoLogin.ftl");
        authenticationFlowContext.challenge(challenge);
    }

    // TODO: 与validateAnswer方法联动实现信息校验
    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        boolean validated = validateAnswer(authenticationFlowContext);
        if (!validated) {
            logger.info("验证失败");
            Response challenge = authenticationFlowContext.form().setError("信息错误").createForm("DeviceInfoLogin.ftl");
            authenticationFlowContext.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }
        logger.info("验证成功");
        authenticationFlowContext.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        logger.info("查看是否配置认证器...");
        boolean flag = getCredentialProvider(keycloakSession).isConfiguredFor(realmModel, userModel, getType(keycloakSession));
        logger.info("配置认证器结果：" + flag);
        return flag;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        logger.info("未配置认证器，但必须，所以设置注册RequiredAction：" + DeviceAuthRequiredAction.PROVIDER_ID);
        userModel.addRequiredAction(DeviceAuthRequiredAction.PROVIDER_ID);
    }

    @Override
    public DeviceAuthCredentialProvider getCredentialProvider(KeycloakSession keycloakSession) {
        return (DeviceAuthCredentialProvider) keycloakSession.getProvider(CredentialProvider.class, DeviceAuthCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public void close() {

    }

    // TODO: 完成信息校验，此处临时只返回true便于开发
    protected boolean validateAnswer(AuthenticationFlowContext authenticationFlowContext) {
        MultivaluedMap<String, String> formData = authenticationFlowContext.getHttpRequest().getDecodedFormParameters();
        // 得到设备信息
        String cpuid = formData.getFirst("cpuid");
        String visitorId = formData.getFirst("device_fingerprint");
        logger.info("设备信息：");
        logger.info("cpuid: " + cpuid);
        logger.info("visitorId: " + visitorId);
        return true;
    }
}
