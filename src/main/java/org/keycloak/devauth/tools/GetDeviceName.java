package org.keycloak.devauth.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class GetDeviceName {
    public String getName() throws UnknownHostException {
        String name = InetAddress.getLocalHost().getHostName();
        return name;
    }
}
