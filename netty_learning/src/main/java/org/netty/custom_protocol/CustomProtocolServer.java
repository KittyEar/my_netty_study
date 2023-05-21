package org.netty.custom_protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;


import java.util.List;

record Data(int length, byte[] content) {

}
public class CustomProtocolServer {
    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new CustomServerInitializer());

            ChannelFuture channelFuture = serverBootstrap.bind(8899).sync();

            channelFuture.channel().closeFuture().sync();
        }catch (Exception exception) {
            exception.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
class CustomDecoder extends ReplayingDecoder<Void> {
    private int length;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (length == 0) {
            length = in.readInt();
        }
        if (in.readableBytes() < length) {//数据没全，设置checkpoint，下次再来
            checkpoint();
            return;
        }
        byte[] content = new byte[length];
        in.readBytes(content);
        Data data = new Data(length, content);
        out.add(data);
        length = 0;
    }
}

class CustomEncoder extends MessageToByteEncoder<Data> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Data msg, ByteBuf out) throws Exception {
        out.writeInt(msg.length());
        out.writeBytes(msg.content());
    }
}
class CustomHandler extends SimpleChannelInboundHandler<Data> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Data msg) throws Exception {
        System.out.println(msg);//业务代码
        System.out.println(new String(msg.content()));
    }
}
class CustomServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new CustomDecoder())
                .addLast(new CustomEncoder())
                .addLast(new CustomHandler());
    }
}