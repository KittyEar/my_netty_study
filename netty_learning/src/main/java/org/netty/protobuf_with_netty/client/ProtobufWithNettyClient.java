package org.netty.protobuf_with_netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.netty.protobuf.DataInfo;

public class ProtobufWithNettyClient {
    private static void protobufWithNettyClient() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();//客户端只需要一个事件循环组

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ProtobufWithNettyClientInitializer());

            ChannelFuture channelFuture = bootstrap.connect("localhost", 8899).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        protobufWithNettyClient();
    }
}
class ProtobufWithNettyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(new ProtobufDecoder(DataInfo.Student_12345.getDefaultInstance()))//参数为protobuf类型
                .addLast(new ProtobufVarint32LengthFieldPrepender())
                .addLast(new ProtobufEncoder())
                .addLast(new ProtobufWithNettyClientHandler());
    }
}
class ProtobufWithNettyClientHandler extends SimpleChannelInboundHandler<DataInfo.Student_12345> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DataInfo.Student_12345 msg) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        DataInfo.Student_12345.Builder message = DataInfo.Student_12345.newBuilder();
        message
                .setName("法外狂徒")
                .setAge(35)
                .setAddress("比尔吉沃特");
        ctx.channel().writeAndFlush(message.build());
    }
}
