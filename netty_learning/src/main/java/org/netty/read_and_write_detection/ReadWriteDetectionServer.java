package org.netty.read_and_write_detection;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 服务器对读写事件的检测，在有客户端连接的时候
 */
public class ReadWriteDetectionServer {
    private static void readWriteDetectionServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();//启动服务器
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ReadWriteDetectionInitializer());

            ChannelFuture channelFuture = serverBootstrap.bind(8899).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        readWriteDetectionServer();
    }
}
class ReadWriteDetectionInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();//pipeline流程就是责任链模式

        pipeline.addLast(new IdleStateHandler(5, 7, 4, TimeUnit.SECONDS))
                .addLast(new ReadWriteDetectionHandler());


    }
}
class ReadWriteDetectionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            String eventType = null;
            
            switch (event.state()) {
                case READER_IDLE -> eventType = "读空闲";
                case WRITER_IDLE -> eventType = "写空闲";
                case ALL_IDLE -> eventType = "读写空闲";//有一段时间没有收到或发送任何数据。
            }

            System.out.println(ctx.channel().remoteAddress() + " 超时事件： " + eventType);
            ctx.channel().close();//超时了都，该关掉了
        }
    }
}
