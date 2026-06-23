package com.cl_labs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolHandlerTest {

    @Test
    void testEncodeDecodeSuccess() throws Exception {
        ProtocolHandler handler = new ProtocolHandler();

        byte bSrc = 1;
        long bPktId = 123456789L;
        int cType = 5;
        int bUserId = 1024;
        String secretData = "{\"action\":\"login\", \"status\":\"ok\"}";
        byte[] plainMessage = secretData.getBytes();

        byte[] packet = handler.encode(bSrc, bPktId, cType, bUserId, plainMessage);
        
        assertNotNull(packet);

        DecodedMessage decoded = handler.decode(packet);

        assertEquals(bSrc, decoded.getbSrc());
        assertEquals(bPktId, decoded.getbPktId());
        assertEquals(cType, decoded.getcType());
        assertEquals(bUserId, decoded.getbUserId());
        assertEquals(secretData, new String(decoded.getMessageData()));
    }

    @Test
    void testCorruptedHeaderCrc() throws Exception {
        ProtocolHandler handler = new ProtocolHandler();
        byte[] packet = handler.encode((byte) 1, 1L, 1, 1, "Test".getBytes());

        packet[1] = 99;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.decode(packet);
        });
        assertTrue(exception.getMessage().contains("CRC16 заголовка"));
    }

    @Test
    void testCorruptedPayloadCrc() throws Exception {
        ProtocolHandler handler = new ProtocolHandler();
        byte[] packet = handler.encode((byte) 1, 1L, 1, 1, "Test".getBytes());

        packet[18] = (byte) (packet[18] ^ 0xFF);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            handler.decode(packet);
        });
        assertTrue(exception.getMessage().contains("CRC16 тіла повідомлення"));
    }
}