package com.cl_labs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class StoreClientTCP {
    private final ProtocolHandler handler = new ProtocolHandler();
    private final String host;
    private final int port;

    public StoreClientTCP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendWithRetry(String data, long packetId, int maxRetries) throws Exception {
        int attempts = 0;
        
        while (attempts < maxRetries) {
            try (Socket socket = new Socket(host, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                byte[] packet = handler.encode((byte) 1, packetId, 100, 101, data.getBytes());
                out.write(packet);
                out.flush();

                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                
                if (bytesRead > 0) {
                    return "Success";
                } else {
                    throw new Exception("З'єднання закрито сервером");
                }

            } catch (Exception e) {
                attempts++;
                System.out.println("Сервер недоступний. Спроба " + attempts + " з " + maxRetries + ". Очікування...");
                Thread.sleep(1000);
            }
        }
        throw new Exception("Не вдалося доставити повідомлення після " + maxRetries + " спроб.");
    }
}