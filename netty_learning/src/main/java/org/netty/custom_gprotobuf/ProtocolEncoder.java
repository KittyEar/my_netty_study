package org.netty.custom_gprotobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtocolEncoder extends MessageToByteEncoder<ProtocolMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getDataLength());
        out.writeInt(msg.getProtocolNumber());
        out.writeBytes(msg.getContent());
    }
}