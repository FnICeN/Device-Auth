package com.DeviceAuthApi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DeviceName {
    private final String deviceName;
    private final String publicKeyJson;
    @JsonCreator
    public DeviceName(@JsonProperty("name") String deviceName, @JsonProperty("publicKey") String publicKeyJson) {
        this.deviceName = deviceName;
        this.publicKeyJson = publicKeyJson;
    }
}
