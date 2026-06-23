package com.cl_labs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerTCP {
    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private volatile boolean isRunning = false;
    private final ProtocolHandler protocolHandler = new ProtocolHandler();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        isRunning = true;
        
        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop() throws IOException {
        isRunning = false;
        threadPool.shutdownNow();
        if (serverSocket != null) serverSocket.close();
    }

    private void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (isRunning) {
                byte[] header = new byte[16];
                in.readFully(header);

                ByteBuffer bb = ByteBuffer.wrap(header);
                bb.position(10);
                int wLen = bb.getInt(); 

                byte[] rest = new byte[wLen + 2];
                in.readFully(rest);

                byte[] fullPacket = new byte[16 + wLen + 2];
                System.arraycopy(header, 0, fullPacket, 0, 16);
                System.arraycopy(rest, 0, fullPacket, 16, rest.length);

                DecodedMessage msg = protocolHandler.decode(fullPacket);
                
                byte[] response = protocolHandler.encode((byte) 2, msg.getbPktId(), 200, 0, "ACK".getBytes());
                out.write(response);
                out.flush();
            }
        } catch (Exception e) {
        }
    }
}