package org.netty.websocket;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class WebSocketServer {
    private static void webSocketServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();//启动服务器
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebSocketInitializer());

            ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(8899)).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        webSocketServer();
    }
}
class WebSocketInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline
                .addLast(new HttpServerCodec())//websocket是基于http的
                .addLast(new ChunkedWriteHandler())//块状输入
                .addLast(new HttpObjectAggregator(8192))//整合Http
                .addLast(new WebSocketServerProtocolHandler("/websocket"))//ws://localhost:8899/websocket
                //.addLast(new TextWebSocketFrameHandler());
                .addLast(new MyWebSocketServerHandler());
    }
}

/**
 * 指定的范型TextWebSocketFrame一般表示消息的类型，还有其他的，如：HttpObject，String
 */
class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        System.out.println("收到消息： " + msg.text());

        ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器时间： " + LocalDateTime.now()));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded: " + ctx.channel().id().asLongText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved: " + ctx.channel().id().asLongText());
    }
}
class MyWebSocketServerHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws Exception {
        ByteBuf buffer = msg.content();
        // 解析消息类型
        int messageTypeLength = buffer.bytesBefore((byte) 0);
        String messageType = buffer.readCharSequence(messageTypeLength, CharsetUtil.UTF_8).toString();
        // 解析消息体
        byte[] messageData = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), messageData);
        System.out.println(messageTypeLength);
        //Message message = myMessageBuilder.mergeFrom(messageData).build();
        // 处理消息
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 在连接建立后，发送欢迎消息
        String welcomeMessage = "Welcome to my WebSocket server!";
        byte[] bytes = welcomeMessage.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(bytes.length);
        buf.writeBytes(bytes);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(buf));
    }

//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        // 在WebSocket握手成功后，添加自定义处理器
//        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
//            ctx.pipeline().addLast(new WebSocketFrameEncoder(), new WebSocketFrameDecoder(), new WebSocketFrameAggregator(65536), this);
//        } else {
//            super.userEventTriggered(ctx, evt);
//        }
//    }

}
