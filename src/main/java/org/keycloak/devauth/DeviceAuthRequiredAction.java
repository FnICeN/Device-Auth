package org.keycloak.devauth;

import com.DeviceAuthApi.DeviceAuthConstants;
import com.DeviceAuthApi.DeviceAuthCredentialModel;
import com.DeviceAuthApi.DeviceAuthCredentialProvider;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.FormMessage;
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

    @Override
    public void requiredActionChallenge(RequiredActionContext requiredActionContext) {
        logger.info("打开添加信息界面...");
        Response response =requiredActionContext.form().createForm("DeviceInfoRegister.ftl");
        requiredActionContext.challenge(response);

    }

    // TODO：报错信息会被重复显示在页面上，待解决
    @Override
    public void processAction(RequiredActionContext requiredActionContext) {
        logger.info("处理添加信息的表单数据...");
        String cpuid = requiredActionContext.getHttpRequest().getDecodedFormParameters().getFirst("cpuid");
        String visitorId = requiredActionContext.getHttpRequest().getDecodedFormParameters().getFirst("device_fingerprint");
        String hostName = requiredActionContext.getHttpRequest().getDecodedFormParameters().getFirst("host_name");
        logger.info("获取到的设备信息：");
        logger.info("cpuid：" + cpuid);
        logger.info("visitorId：" + visitorId);
        logger.info("hostName：" + hostName);
        if (cpuid.isEmpty() || visitorId.isEmpty() || hostName.isEmpty()) {
            requiredActionContext.form().addError(new FormMessage("host_name", "主机名不可为空"));
            requiredActionContext.challenge(requiredActionContext.form().createForm("DeviceInfoRegister.ftl"));
            return;
        }
        DeviceAuthCredentialProvider dacp = (DeviceAuthCredentialProvider) requiredActionContext.getSession().getProvider(CredentialProvider.class, DeviceAuthConstants.credentialProviderFactoryID);
        // 将信息存储下来
        dacp.createCredential(requiredActionContext.getRealm(), requiredActionContext.getUser(), DeviceAuthCredentialModel.createDeviceAuth(hostName, cpuid, visitorId));
        logger.info("已存储凭证信息：hostName=" + hostName + "cpuid=" + cpuid + " visitorId=" + visitorId);
        requiredActionContext.success();
    }

    @Override
    public void close() {

    }
}
