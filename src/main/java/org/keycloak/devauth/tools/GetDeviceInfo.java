package org.keycloak.devauth.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

public class GetDeviceInfo {
    public String getCpuId() throws IOException {
        String cpuId;
        String os = System.getProperty("os.name");
        if (os.equals("LINUX")) {
            cpuId = getLinuxCpuId("dmidecode -t processor | grep 'ID'", "ID", ":");
        } else {
            cpuId = getWindowsCpuId();
        }
        return cpuId;
    }
    private String getLinuxCpuId(String cmd, String record, String symbol) throws IOException {
        String execResult = executeLinuxCmd(cmd);
        String[] infos = execResult.split("\n");
        for (String info : infos) {
            info = info.trim();
            if (info.contains(record)) {
                info = info.replace(" ", "");
                String[] sn = info.split(symbol);
                return sn[1];
            }
        }
        return null;
    }

    private String executeLinuxCmd(String cmd) throws IOException {
        Runtime run = Runtime.getRuntime();
        Process process = run.exec(cmd);
        InputStream in = process.getInputStream();
        BufferedReader bs = new BufferedReader(new InputStreamReader(in));
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[8192];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        in.close();
        process.destroy();
        return out.toString();
    }
    private String getWindowsCpuId() throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
        process.getOutputStream().close();
        Scanner sc = new Scanner(process.getInputStream());
        sc.next();
        String res = sc.next();
        return res;
    }
}
