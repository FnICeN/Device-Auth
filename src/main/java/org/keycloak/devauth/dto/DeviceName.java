package org.keycloak.devauth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DeviceName {
    private final String deviceName;
    @JsonCreator
    public DeviceName(@JsonProperty("name") String deviceName) {
        this.deviceName = deviceName;
    }
}
