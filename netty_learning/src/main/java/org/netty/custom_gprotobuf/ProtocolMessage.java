package org.netty.custom_gprotobuf;

import java.util.Arrays;

public class ProtocolMessage {
    private final int dataLength;
    private final int protocolNumber;
    private final byte[] content;

    public ProtocolMessage(int dataLength, int protocolNumber, byte[] content) {
        this.dataLength = dataLength;
        this.protocolNumber = protocolNumber;
        this.content = content;
    }

    public int getDataLength() {
        return dataLength;
    }

    public int getProtocolNumber() {
        return protocolNumber;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ProtocolMessage{" +
                "dataLength=" + dataLength +
                ", protocolNumber=" + protocolNumber +
                ", content=" + Arrays.toString(content) +
                '}';
    }
}
