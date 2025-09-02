package org.keycloak.devauth;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

public class DeviceAuthAuthenticator implements Authenticator, CredentialValidator<DeviceAuthCredentialProvider> {
    private Logger logger = Logger.getLogger(DeviceAuthAuthenticator.class);
    private int credentialNum = 3;
    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        List<CredentialModel> credentials = authenticationFlowContext.getUser().credentialManager().getStoredCredentialsByTypeStream("DEVICE_AUTH").collect(Collectors.toList());
        authenticationFlowContext.form().setAttribute("credentials", credentials);
        credentialNum = credentials.size();
        logger.info("凭证数量：" + credentialNum);
        logger.info("首个凭证userLabel：" + credentials.getFirst().getUserLabel());
        logger.info("首个凭证secret：" + credentials.getFirst().getSecretData());
        logger.info("进入认证，显示页面...");
        Response challenge = authenticationFlowContext.form().createForm("DeviceInfoLogin.ftl");
        authenticationFlowContext.challenge(challenge);
    }

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

    protected boolean validateAnswer(AuthenticationFlowContext authenticationFlowContext) {
        MultivaluedMap<String, String> formData = authenticationFlowContext.getHttpRequest().getDecodedFormParameters();
        // 得到提交的设备信息
        String cpuid = formData.getFirst("cpuid");
        String visitorId = formData.getFirst("device_fingerprint");
        String credentialId = formData.getFirst("credentialId");
        boolean save = formData.getFirst("recordDeviceInfo") != null;
        logger.info("凭据ID：" + credentialId);
        logger.info("设备信息：");
        logger.info("cpuid: " + cpuid);
        logger.info("visitorId: " + visitorId);
        logger.info("是否保存: " + save);

        String challengeResponse = cpuid + "||" + visitorId;

        UserCredentialModel input = new UserCredentialModel(credentialId, getType(authenticationFlowContext.getSession()), challengeResponse);
        boolean isValid = getCredentialProvider(authenticationFlowContext.getSession()).isValid(authenticationFlowContext.getRealm(), authenticationFlowContext.getUser(), input);
        // 用户选择注册新设备
        if (save && !isValid) {
            registerNewDevice(authenticationFlowContext, cpuid, visitorId);
            return true;
        }
        return isValid;
    }

    private void registerNewDevice(AuthenticationFlowContext authenticationFlowContext, String cpuid, String visitorId) {
        if (credentialNum >= 3) {
            Response challenge = authenticationFlowContext.form().setError("凭证超上限").createForm("DeviceInfoLogin.ftl");
            authenticationFlowContext.failureChallenge(AuthenticationFlowError.ACCESS_DENIED, challenge);
        }
        logger.info("选择注册且此设备原本无效，此认证直接通过，将信息传递至下一个认证器...");
        authenticationFlowContext.getAuthenticationSession().setClientNote("registeringDevice", "true");
        authenticationFlowContext.getAuthenticationSession().setClientNote("cpuid", cpuid);
        authenticationFlowContext.getAuthenticationSession().setClientNote("visitorId", visitorId);
    }
}
