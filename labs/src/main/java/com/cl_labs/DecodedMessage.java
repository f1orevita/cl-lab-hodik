package com.cl_labs;

public class DecodedMessage {
    private final byte bSrc;
    private final long bPktId;
    private final int cType;
    private final int bUserId;
    private final byte[] messageData;

    public DecodedMessage(byte bSrc, long bPktId, int cType, int bUserId, byte[] messageData) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.cType = cType;
        this.bUserId = bUserId;
        this.messageData = messageData;
    }

    public byte getbSrc() { return bSrc; }
    public long getbPktId() { return bPktId; }
    public int getcType() { return cType; }
    public int getbUserId() { return bUserId; }
    public byte[] getMessageData() { return messageData; }
}