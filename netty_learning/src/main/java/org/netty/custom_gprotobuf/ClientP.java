package org.netty.custom_gprotobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.netty.protobuf.DataInfo;

public class ClientP {
    private final String host;
    private final int port;

    public ClientP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtocolEncoder())
                                    .addLast(new ProtocolDecoder())
                                    .addLast(new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ClientP client = new ClientP("localhost", 8899);
        client.run();
    }
    private static class ClientHandler extends SimpleChannelInboundHandler<ProtocolMessage> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 当与服务器建立连接后，发送消息
            DataInfo.Student_12345 student12345 = DataInfo.Student_12345.newBuilder().setAge(18).setAddress("cn").setName("gg").build();
            ProtocolMessage message = new ProtocolMessage(student12345.getSerializedSize(), 12345, student12345.toByteArray());
            ctx.writeAndFlush(message);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) throws Exception {
            // 收到服务器的响应消息
            System.out.println("Received message: " + new String(msg.getContent()));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

}
