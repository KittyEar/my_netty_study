package org.netty.chatFunction.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ChatServer {
    private static void chatServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();//启动服务器
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChatServerInitializer());

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
        chatServer();
    }
}
class ChatServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline
                .addLast(new DelimiterBasedFrameDecoder(4096, Delimiters.lineDelimiter()))
                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                .addLast(new StringEncoder(CharsetUtil.UTF_8))
                .addLast(new ChatServerHandler())
                .addLast(new ExceptionHandler());
    }
}
class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    //表示加入服务器的所有channel集合，必须要尽快初始化，不然会导致其他channel无法加入其中
    protected static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 处理当前channel的所传来的msg，要和其他客户端区分
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     *
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel clientChannel = ctx.channel();//消息源
        //转发消息
        channelGroup.forEach(ch -> {
            if (ch.compareTo(clientChannel) == 0) {
                ch.writeAndFlush("myself" + ":" + msg + "\n");
            } else {
                ch.writeAndFlush(ch.remoteAddress() + ":" + msg +"\n");
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();//刚加入的客户端channel实例
        System.out.println("message from server: " + clientChannel.remoteAddress() + " online");
    }

    /**
     * 有新的客户端建立连接时会调用，每一个客户端都用一个channel表示
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();//刚加入的客户端channel实例
        //给所有客户端发通知
        channelGroup.writeAndFlush("message from server: " + clientChannel.remoteAddress() + " join in\n");
        //将新来的客户端加入集合
        channelGroup.add(clientChannel);
    }

    /**
     * 客户端离线
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();//刚加入的客户端channel实例
        //给所有客户端发通知
        channelGroup.writeAndFlush("message from server: " + clientChannel.remoteAddress() + " leave\n");

        channelGroup.remove(clientChannel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel clientChannel = ctx.channel();//刚加入的客户端channel实例
        System.out.println("message from server: " + clientChannel.remoteAddress() + " offline\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
class ExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println(this.getClass().getName() + " 异常处理,e:" + cause);
    }
}
