package com.cl_labs;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class StoreClientUDP {
    private final ProtocolHandler handler = new ProtocolHandler();
    private final String host;
    private final int port;

    public StoreClientUDP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendWithRetry(String data, long packetId, int maxRetries) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1000);
            InetAddress serverAddress = InetAddress.getByName(host);

            byte[] payload = handler.encode((byte) 1, packetId, 1, 102, data.getBytes());
            DatagramPacket sendPacket = new DatagramPacket(payload, payload.length, serverAddress, port);

            int attempts = 0;
            while (attempts < maxRetries) {
                try {
                    attempts++;
                    socket.send(sendPacket);

                    byte[] receiveBuffer = new byte[2048];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);

                    return "Success";
                    
                } catch (SocketTimeoutException e) {
                    System.out.println("Таймаут UDP. Спроба " + attempts + " з " + maxRetries + " провалилася. Переповтор...");
                }
            }
            throw new Exception("Не вдалося доставити UDP пакет після " + maxRetries + " спроб.");
        }
    }
}