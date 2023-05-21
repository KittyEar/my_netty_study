package org.netty.custom_protocol;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.util.stream.IntStream;


public class CustomProtocolClient {
    public static void main(String[] args) {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();//客户端只需要一个事件循环组
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new CustomClientInitializer());

            ChannelFuture channelFuture = bootstrap.connect("localhost", 8899).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
class CustomClientInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new CustomDecoder())
                .addLast(new CustomEncoder())
                .addLast(new ClientHandler());
    }


}
class ClientHandler extends SimpleChannelInboundHandler<Data> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Data msg) throws Exception {

    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        IntStream.range(0, 10).forEach(i -> {
            byte[] bytes = "hello".getBytes(CharsetUtil.UTF_8);
            Data data = new Data(bytes.length, bytes);
            ctx.writeAndFlush(data);
        });
    }
}