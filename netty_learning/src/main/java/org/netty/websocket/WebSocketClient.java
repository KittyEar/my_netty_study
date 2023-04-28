package org.netty.websocket;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import org.netty.protobuf.DataInfo;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class WebSocketClientHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private final DataInfo.Student_23456.Builder messageBuilder;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
        this.messageBuilder = DataInfo.Student_23456.newBuilder(); // 替换成你的消息类型
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) throws Exception {
        ByteBuf buffer = frame.content();
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        String messageType = new String(Arrays.copyOf(bytes, bytes.length - DataInfo.Student_23456.getDefaultInstance().getSerializedSize()), StandardCharsets.UTF_8);
        DataInfo.Student_23456 message = messageBuilder.mergeFrom(bytes, messageType.length() + 1, bytes.length - messageType.length() - 1).build();
        // 处理消息
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

}
public class WebSocketClient {

    public static void main(String[] args) throws Exception {
        URI uri = new URI("ws://localhost:8899/websocket");
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            //pipeline.addLast(new WebSocketClientProtocolHandler(handshaker));
                            pipeline.addLast(new WebSocketClientHandler(handshaker));
                        }
                    });

            Channel channel = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            WebSocketClientHandler handler = (WebSocketClientHandler) channel.pipeline().last();
            handler.handshakeFuture().sync();

            DataInfo.Student_23456 message = DataInfo.Student_23456.newBuilder() // 替换成你的消息类型
                    .setName("Alice")
                    .setAddress("第三方")
                    .setAge(324)
                    .build();

            String messageType = "your.package.MyMessage";
            byte[] messageTypeBytes = messageType.getBytes(StandardCharsets.UTF_8);
            byte[] messageBytes = message.toByteArray();
            byte[] payload = new byte[messageTypeBytes.length + 1 + messageBytes.length];
            System.arraycopy(messageTypeBytes, 0, payload, 0, messageTypeBytes.length);
            payload[messageTypeBytes.length] = 0;
            System.arraycopy(messageBytes, 0, payload, messageTypeBytes.length + 1, messageBytes.length);
            channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(payload)));
        }catch (Exception e) {

        }
    }
}
