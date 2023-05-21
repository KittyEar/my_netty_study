package org.netty.custom_gprotobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.netty.protobuf.DataInfo;

import java.util.List;

public class ProtocolDecoder extends ByteToMessageDecoder {
    private static final int HEADER_SIZE = 6;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEADER_SIZE) {
            return;
        }

        int dataLength = in.readUnsignedShort();
        int protocolNumber = in.readInt();

        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] content = new byte[dataLength];
        in.readBytes(content);

        ProtocolMessage message = new ProtocolMessage(dataLength, protocolNumber, content);
        out.add(message);
    }
}