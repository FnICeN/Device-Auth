package org.keycloak.devauth;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

public class DeviceAuthRequiredAction implements RequiredActionProvider, CredentialRegistrator {
    private Logger logger = Logger.getLogger(DeviceAuthRequiredAction.class);
    public static final String PROVIDER_ID = "device-auth-config";
    @Override
    public String getCredentialType(KeycloakSession keycloakSession, AuthenticationSessionModel authenticationSessionModel) {
        return DeviceAuthCredentialModel.TYPE;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext requiredActionContext) {

    }

    // TODO：允许用户添加多个凭证数据
    @Override
    public void requiredActionChallenge(RequiredActionContext requiredActionContext) {
        logger.info("打开添加信息界面...");
        Response response =requiredActionContext.form().createForm("DeviceInfoRegister.ftl");
        requiredActionContext.challenge(response);

    }

    // TODO：暂定设备名为host1，之后需要改动
    @Override
    public void processAction(RequiredActionContext requiredActionContext) {
        logger.info("处理添加信息的表单数据...");
        String cpuid = requiredActionContext.getHttpRequest().getDecodedFormParameters().getFirst("cpuid");
        String visitorId = requiredActionContext.getHttpRequest().getDecodedFormParameters().getFirst("device_fingerprint");
        logger.info("获取到的设备信息：");
        logger.info("cpuid：" + cpuid);
        logger.info("visitorId：" + visitorId);
        logger.info("暂定设备名为host1");
        DeviceAuthCredentialProvider dacp = (DeviceAuthCredentialProvider) requiredActionContext.getSession().getProvider(CredentialProvider.class, "device-auth");
        // 将信息存储下来
        dacp.createCredential(requiredActionContext.getRealm(), requiredActionContext.getUser(), DeviceAuthCredentialModel.createDeviceAuth("host1", cpuid, visitorId));
        logger.info("已存储host1凭证信息：cpuid=" + cpuid + " visitorId=" + visitorId);
        requiredActionContext.success();
    }

    @Override
    public void close() {

    }
}
