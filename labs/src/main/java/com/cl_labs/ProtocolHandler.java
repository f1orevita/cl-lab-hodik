package com.cl_labs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ProtocolHandler {
    private static final byte MAGIC_BYTE = 0x13;

    public byte[] encode(byte bSrc, long bPktId, int cType, int bUserId, byte[] plainMessage) throws Exception {
        byte[] encryptedMessage = CryptoUtils.encrypt(plainMessage);
        
        int wLen = 8 + encryptedMessage.length;

        ByteBuffer header = ByteBuffer.allocate(14);
        header.order(ByteOrder.BIG_ENDIAN);
        header.put(MAGIC_BYTE);
        header.put(bSrc);
        header.putLong(bPktId);
        header.putInt(wLen);

        short headerCrc = CRC16.calculate(header.array(), 0, 14);

        ByteBuffer payload = ByteBuffer.allocate(wLen);
        payload.order(ByteOrder.BIG_ENDIAN);
        payload.putInt(cType);
        payload.putInt(bUserId);
        payload.put(encryptedMessage);

        short payloadCrc = CRC16.calculate(payload.array(), 0, wLen);

        ByteBuffer packet = ByteBuffer.allocate(16 + wLen + 2);
        packet.order(ByteOrder.BIG_ENDIAN);
        packet.put(header.array());
        packet.putShort(headerCrc);
        packet.put(payload.array());
        packet.putShort(payloadCrc);

        return packet.array();
    }

    public DecodedMessage decode(byte[] packetBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(packetBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        byte bMagic = buffer.get();
        if (bMagic != MAGIC_BYTE) {
            throw new IllegalArgumentException("Невірний початковий байт пакету (bMagic)");
        }

        byte bSrc = buffer.get();
        long bPktId = buffer.getLong();
        int wLen = buffer.getInt();

        short expectedHeaderCrc = CRC16.calculate(packetBytes, 0, 14);
        short actualHeaderCrc = buffer.getShort();
        if (expectedHeaderCrc != actualHeaderCrc) {
            throw new IllegalArgumentException("Помилка цілісності: CRC16 заголовка не збігається");
        }

        short expectedPayloadCrc = CRC16.calculate(packetBytes, 16, wLen);
        buffer.position(16 + wLen);
        short actualPayloadCrc = buffer.getShort();
        if (expectedPayloadCrc != actualPayloadCrc) {
            throw new IllegalArgumentException("Помилка цілісності: CRC16 тіла повідомлення не збігається");
        }

        buffer.position(16);
        int cType = buffer.getInt();
        int bUserId = buffer.getInt();

        int encryptedMsgLen = wLen - 8;
        byte[] encryptedMessage = new byte[encryptedMsgLen];
        buffer.get(encryptedMessage);

        byte[] decryptedMessage = CryptoUtils.decrypt(encryptedMessage);

        return new DecodedMessage(bSrc, bPktId, cType, bUserId, decryptedMessage);
    }
}