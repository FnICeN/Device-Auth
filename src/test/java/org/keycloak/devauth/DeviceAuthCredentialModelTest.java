package org.keycloak.devauth;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.devauth.dto.DeviceName;

public class DeviceAuthCredentialModelTest {
    DeviceAuthCredentialModel dacm = DeviceAuthCredentialModel.createDeviceAuth("Admin", "ABCDEFG", "73c6b82465554e9c5fc4b76f11ac99c6");
    @Test
    public void modelCreateTest() {
        Assert.assertEquals("Admin", dacm.getDeviceName().getDeviceName());
        Assert.assertEquals("ABCDEFG", dacm.getDeviceData().getCpuId());
        Assert.assertEquals("73c6b82465554e9c5fc4b76f11ac99c6", dacm.getDeviceData().getVisitorId());
    }
    @Test
    public void modelPropertiesTest() {
        Assert.assertEquals("{\"deviceName\":\"Admin\"}", dacm.getCredentialData());
        Assert.assertEquals("{\"visitorId\":\"73c6b82465554e9c5fc4b76f11ac99c6\",\"cpuId\":\"ABCDEFG\"}", dacm.getSecretData());
        Assert.assertEquals("DEVICE_AUTH", dacm.getType());
        System.out.println(dacm.getCreatedDate());
    }
}
