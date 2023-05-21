package org.netty.firstexample;

import static org.junit.Assert.assertTrue;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class ChannelInitializerDemo {
    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new MyChannelInitializer());

        Channel channel = bootstrap.connect(new InetSocketAddress("localhost", 8080)).sync().channel();
        // 等待一段时间以确保 ChannelInitializer 已经被移除
        Thread.sleep(1000);
        System.out.println("Is ChannelInitializer still in pipeline? " +
                (channel.pipeline().get(MyChannelInitializer.class) != null));
        channel.close().sync();
    }

    private static class MyChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(new MyHandler());
        }
    }

    private static class MyHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("MyHandler.channelActive called");
            // 手动删除 ChannelInitializer
            ctx.pipeline().remove(MyChannelInitializer.class);
        }
    }
}


