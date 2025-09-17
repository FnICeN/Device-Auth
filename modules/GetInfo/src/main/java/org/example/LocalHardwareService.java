package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class LocalHardwareService {
    static Crypto crypto;
    public static void main(String[] args) throws Exception {
        crypto = new Crypto();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 12345), 0);
        server.createContext("/get_cpuid", new CPUIDHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("服务已启动，监听 127.0.0.1:12345");
    }

    static class CPUIDHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String nonce = exchange.getRequestURI().getQuery();
            System.out.println(nonce);

            GetDeviceInfo gdi = new GetDeviceInfo();
            String cpuid = gdi.getCpuId();
            String response;

            if (nonce == null || nonce.isEmpty()) {
                String publicKeyJson = crypto.getPublicKeysetString();
                Map<String, Object> map = new HashMap<>();
                map.put("cpuid", cpuid);
                map.put("publicKeyJson", publicKeyJson); // publicKeyJson 是字符串
                ObjectMapper objectMapper = new ObjectMapper();
                response = objectMapper.writeValueAsString(map);

            } else {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String signed = crypto.sign(cpuid, timestamp, nonce);
                response = String.format("{\"sign\":\"%s\", \"timestamp\":\"%s\"}", signed, timestamp);
            }

            System.out.println(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            // 针对本地前端请求时跨域问题
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }
}

