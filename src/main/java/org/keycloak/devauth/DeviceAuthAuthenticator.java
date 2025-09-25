package org.keycloak.devauth;

import com.DeviceAuthApi.DeviceAuthConstants;
import com.DeviceAuthApi.DeviceAuthCredentialProvider;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeviceAuthAuthenticator implements Authenticator, CredentialValidator<DeviceAuthCredentialProvider> {
    private Logger logger = Logger.getLogger(DeviceAuthAuthenticator.class);
    private int credentialNum = 3;
    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        List<CredentialModel> credentials = authenticationFlowContext.getUser().credentialManager().getStoredCredentialsByTypeStream("DEVICE_AUTH").collect(Collectors.toList());
        authenticationFlowContext.form().setAttribute("credentials", credentials);
        setNonceCookie(authenticationFlowContext);  // 设置随机字符串
        credentialNum = credentials.size();
        logger.info("凭证数量：" + credentialNum);
        logger.info("首个凭证userLabel：" + credentials.get(0).getUserLabel());
        logger.info("首个凭证secret：" + credentials.get(0).getSecretData());
        logger.info("进入认证，显示页面...");
        Response challenge = authenticationFlowContext.form().createForm("DeviceInfoLogin.ftl");
        authenticationFlowContext.challenge(challenge);
    }

    // TODO：验证失败后报错信息会取代选择框，待解决
    // TODO：若用户错选设备且同意注册，则导致冗余注册
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
        return (DeviceAuthCredentialProvider) keycloakSession.getProvider(CredentialProvider.class, DeviceAuthConstants.credentialProviderFactoryID);
    }

    @Override
    public void close() {

    }

    protected boolean validateAnswer(AuthenticationFlowContext authenticationFlowContext) {
        MultivaluedMap<String, String> formData = authenticationFlowContext.getHttpRequest().getDecodedFormParameters();
        // 得到提交的设备信息
        String cpuid = formData.getFirst("cpuid");
        String visitorId = formData.getFirst("device_fingerprint");
        String publicKeyJson = formData.getFirst("public_key");
        String credentialId = formData.getFirst("credentialId");
        String signature = formData.getFirst("signature");
        String timestamp = formData.getFirst("timestamp");
        String nonce = getNonceFromCookie(authenticationFlowContext);
        boolean save = formData.getFirst("recordDeviceInfo") != null;
        String newName = formData.getFirst("new_device_name");
        logger.info("凭据ID：" + credentialId);
        logger.info("设备信息：");
        logger.info("cpuid: " + cpuid);
        logger.info("visitorId: " + visitorId);
        logger.info("publicKeyJson: " + publicKeyJson);
        logger.info("signature: " + signature);
        logger.info("timestamp: " + timestamp);
        logger.info("nonce：" + nonce);
        logger.info("是否保存: " + save);
        logger.info("新设备名：" + newName) ;

        String challengeResponse = signature + "||" + visitorId + "||" + timestamp + "||" + nonce;

        UserCredentialModel input = new UserCredentialModel(credentialId, getType(authenticationFlowContext.getSession()), challengeResponse);
        boolean isValid = getCredentialProvider(authenticationFlowContext.getSession()).isValid(authenticationFlowContext.getRealm(), authenticationFlowContext.getUser(), input);
        // 用户选择注册新设备
        if (save && !isValid) {
            if (newName.isEmpty()) return false;
            registerNewDevice(authenticationFlowContext, newName, cpuid, visitorId, publicKeyJson);
            return true;
        }
        return isValid;
    }

    private void registerNewDevice(AuthenticationFlowContext authenticationFlowContext, String newName, String cpuid, String visitorId, String publicKeyJson) {
        if (credentialNum >= 3) {
            Response challenge = authenticationFlowContext.form().setError("凭证超上限").createForm("DeviceInfoLogin.ftl");
            authenticationFlowContext.failureChallenge(AuthenticationFlowError.ACCESS_DENIED, challenge);
        }
        logger.info("选择注册且此设备原本无效，此认证直接通过，将信息传递至下一个认证器...");
        authenticationFlowContext.getAuthenticationSession().setClientNote("registeringDevice", "true");
        authenticationFlowContext.getAuthenticationSession().setClientNote("newName", newName);
        authenticationFlowContext.getAuthenticationSession().setClientNote("cpuid", cpuid);
        authenticationFlowContext.getAuthenticationSession().setClientNote("visitorId", visitorId);
        authenticationFlowContext.getAuthenticationSession().setClientNote("publicKeyJson", publicKeyJson);
    }

    private void setNonceCookie(AuthenticationFlowContext context) {
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        URI uri = context.getUriInfo().getBaseUriBuilder().path("realms").path(context.getRealm().getName()).build();
        NewCookie newCookie = new NewCookie.Builder("DEV_NONCE").value(nonce)
                .path(uri.getRawPath())
                .secure(false)
                .build();
        context.getSession().getContext().getHttpResponse().setCookieIfAbsent(newCookie);
        // String oidcNonce = context.getAuthenticationSession().getClientNote("nonce");
        // oidc认证时也会在上下文设置一个nonce，需要保证不会被同名覆盖，否则会导致oidc失败（其实设置了也没人会从上下文读取，都是从Cookie获取的）
        // TODO：也许这里不需要设置上下文信息？
        context.getAuthenticationSession().setClientNote("dev_nonce", nonce);
        logger.info("保存nonce：" + nonce);
    }

    private String getNonceFromCookie(AuthenticationFlowContext context) {
        Cookie cookie = context.getHttpRequest().getHttpHeaders().getCookies().get("DEV_NONCE");
        return cookie.getValue().trim();
    }
}
