package org.netty.custom_gprotobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.netty.protobuf.DataInfo;
import org.netty.util.MessageParser;

public class ServerP {
    private final int port;

    public ServerP(int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtocolEncoder())
                                    .addLast(new ProtocolDecoder())
                                    .addLast(new ServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ServerP server = new ServerP(8899);
        server.run();
    }
    private static class ServerHandler extends SimpleChannelInboundHandler<ProtocolMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
            // 处理接收到的协议消息
            //System.out.println("Received message: " + new String(msg.getContent()));
            //System.out.println(msg.getProtocolNumber());
            System.out.println(new MessageParser().parse(msg.getProtocolNumber(), msg.getContent()).toString());
            // 构造响应消息
            byte[] responseContent = "Response".getBytes();
            ProtocolMessage response = new ProtocolMessage(responseContent.length, 5678, responseContent);

            // 发送响应消息
            ctx.writeAndFlush(response);

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
