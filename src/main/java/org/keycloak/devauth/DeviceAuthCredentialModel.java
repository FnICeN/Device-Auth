package org.keycloak.devauth;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.devauth.dto.DeviceData;
import org.keycloak.devauth.dto.DeviceName;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class DeviceAuthCredentialModel extends CredentialModel {
    public static final String TYPE = "DEVICE_AUTH";
    private final DeviceName deviceName;  // 设备名POJO，作为CredentialData
    private final DeviceData deviceData;  // 设备信息POJO，作为SecretData

    public DeviceName getDeviceName() {
        return deviceName;
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    // 由已有POJO构建Model
    private DeviceAuthCredentialModel(DeviceName deviceName, DeviceData deviceData) {
        this.deviceName = deviceName;
        this.deviceData = deviceData;
    }

    // 当场创建POJO再构建Model
    private DeviceAuthCredentialModel(String deviceName, String cpuId, String visitorId) {
        this.deviceName = new DeviceName(deviceName);
        this.deviceData = new DeviceData(cpuId, visitorId);
    }

    public static DeviceAuthCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            // 这里的get方法得到的是Model构建时set方法所设定的数据
            DeviceName deviceName = JsonSerialization.readValue(credentialModel.getCredentialData(), DeviceName.class);
            DeviceData deviceData = JsonSerialization.readValue(credentialModel.getSecretData(), DeviceData.class);
            // 使用传入的已有POJO构建Model，然后填入各属性
            DeviceAuthCredentialModel deviceAuthCredentialModel = new DeviceAuthCredentialModel(deviceName, deviceData);
            deviceAuthCredentialModel.setUserLabel(credentialModel.getUserLabel());
            deviceAuthCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            deviceAuthCredentialModel.setType(credentialModel.getType());
            deviceAuthCredentialModel.setId(credentialModel.getId());
            deviceAuthCredentialModel.setCredentialData(credentialModel.getCredentialData());
            deviceAuthCredentialModel.setSecretData(credentialModel.getSecretData());
            return deviceAuthCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DeviceAuthCredentialModel createDeviceAuth(String deviceName, String cpuId, String visitorId) {
        DeviceAuthCredentialModel dacm = new DeviceAuthCredentialModel(deviceName, cpuId, visitorId);
        dacm.fillFields();
        return dacm;
    }

    private void fillFields() {
        try {
            // 将设备名作为CredentialData，目的是为了之后区分多个不同凭证
            setCredentialData(JsonSerialization.writeValueAsString(deviceName));
            setSecretData(JsonSerialization.writeValueAsString(deviceData));
            setType(this.TYPE);
            setCreatedDate(Time.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
