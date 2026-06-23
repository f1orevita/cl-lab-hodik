package com.cl_labs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class StoreClientTCP {
    private final ProtocolHandler handler = new ProtocolHandler();
    private final String host;
    private final int port;

    public StoreClientTCP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendCommand(int command, String jsonData, long packetId, int maxRetries) throws Exception {
        int attempts = 0;
        
        while (attempts < maxRetries) {
            try (Socket socket = new Socket(host, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                byte[] packet = handler.encode((byte) 1, packetId, command, 101, jsonData.getBytes());
                out.write(packet);
                out.flush();

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

                DecodedMessage msg = handler.decode(fullPacket);
                
                if (msg.getcType() == Processor.CMD_ERROR) {
                    throw new Exception("Помилка від сервера: " + new String(msg.getMessageData()));
                }

                return new String(msg.getMessageData());

            } catch (Exception e) {
                attempts++;
                System.out.println("Помилка зв'язку. Спроба " + attempts + " з " + maxRetries);
                Thread.sleep(1000);
            }
        }
        throw new Exception("Не вдалося доставити повідомлення після " + maxRetries + " спроб.");
    }
}