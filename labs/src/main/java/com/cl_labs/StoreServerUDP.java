package com.cl_labs;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class StoreServerUDP {
    private DatagramSocket socket;
    private volatile boolean isRunning = false;
    private final ProtocolHandler protocolHandler = new ProtocolHandler();
    
    private boolean simulatePacketLoss = false;

    public void setSimulatePacketLoss(boolean simulate) {
        this.simulatePacketLoss = simulate;
    }

    public void start(int port) throws SocketException {
        socket = new DatagramSocket(port);
        isRunning = true;

        new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (isRunning) {
                try {
                    DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(requestPacket);

                    byte[] receivedData = new byte[requestPacket.getLength()];
                    System.arraycopy(requestPacket.getData(), 0, receivedData, 0, requestPacket.getLength());

                    DecodedMessage msg = protocolHandler.decode(receivedData);

                    // ШТУЧНА ВТРАТА ПАКЕТУ (ДЛЯ ТЕСТІВ)
                    if (simulatePacketLoss) {
                        simulatePacketLoss = false;
                        System.out.println("[UDP Сервер] Навмисно проігноровано пакет ID " + msg.getbPktId());
                        continue;
                    }

                    byte[] ackData = protocolHandler.encode((byte) 2, msg.getbPktId(), 2, 0, "ACK".getBytes());
                    DatagramPacket responsePacket = new DatagramPacket(ackData, ackData.length, requestPacket.getAddress(), requestPacket.getPort());
                    socket.send(responsePacket);

                } catch (Exception e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}