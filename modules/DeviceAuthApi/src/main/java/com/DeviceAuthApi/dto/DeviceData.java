package com.DeviceAuthApi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DeviceData {
    private final String cpuId;
    private final String visitorId;

    @JsonCreator
    public DeviceData(@JsonProperty("cpu") String cpuId, @JsonProperty("visitorId") String visitorId) {
        this.cpuId = cpuId;
        this.visitorId = visitorId;
    }
}
