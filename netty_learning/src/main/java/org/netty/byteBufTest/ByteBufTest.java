package org.netty.byteBufTest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ByteBufTest {
    public static void main(String[] args) {
        ByteBuf byteBuf = Unpooled.buffer(10);
        IntStream.range(0, 10).forEach(byteBuf::writeByte);

        System.out.println(Arrays.toString(byteBuf.array()));

        ByteBuf byteBuf1 = Unpooled.copiedBuffer("你好p", CharsetUtil.UTF_8);
        System.out.println(byteBuf1);
    }
}
