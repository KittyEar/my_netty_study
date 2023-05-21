package org.netty.codecTest;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ByteMessageToLongDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        System.out.println("将字节解码成long类型");
        System.out.println(in.readableBytes());//没有被读过的字节数
        if (in.readableBytes() >= 8) { //long类型是8字节，小于8肯定消息不全
            out.add(in.readLong());
        }
    }
}
