package org.netty.protobuf_with_netty.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.TextFormat;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.netty.protobuf.DataInfo;

import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtobufWithNettyServer {
    private static void protobufWithNettyServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();//启动服务器
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ProtobufWithNettyServerInitializer());

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
        protobufWithNettyServer();
    }
}
class ProtobufWithNettyServerInitializer extends ChannelInitializer<SocketChannel> {

    public static MessageParser messageParser;

    public ProtobufWithNettyServerInitializer() {
       //method1();
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(new ProtobufDecoder(DataInfo.Student_12345.getDefaultInstance()))//参数为protobuf类型
                .addLast(new ProtobufVarint32LengthFieldPrepender())
                .addLast(new ProtobufEncoder())
                .addLast(new MyHandler());
                //.addLast(new ProtobufWithNettyServerHandler());
    }
}

//class ProtobufWithNettyServerHandler extends SimpleChannelInboundHandler<DataInfo.Student> {
//
//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, DataInfo.Student msg) throws Exception {
//        String typeName = msg.getDescriptorForType().getName();
//        String values = msg
//                .getAllFields()
//                .entrySet()
//                .stream()
//                .map(entry -> entry.getKey().getName() + "=" + entry.getValue())
//                .collect(Collectors.joining(", ", "{", "}"));
//
//        System.out.println(typeName + values);
//    }
//}
class MessageParser {

    public Map<Integer, MessageLite> messageTypes = new HashMap<>();

    public void registerMessageType(int protocolNumber, MessageLite messageLite) {
        messageTypes.put(protocolNumber, messageLite);
    }

    public MessageLite parse(int protocolNumber, byte[] data) throws InvalidProtocolBufferException {
        MessageLite messageLite = messageTypes.get(protocolNumber);
        if (messageLite == null) {
            throw new IllegalArgumentException("Unknown protocol number: " + protocolNumber);
        }
        return messageLite.newBuilderForType().mergeFrom(data).build();
    }
}
class MyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private MessageParser messageParser;
    private void initParser() {
        // 初始化MessageParser对象
        messageParser = new MessageParser();

// 遍历指定目录下的所有class文件
        String packageName = "org.netty.protobuf";
        String packagePath = packageName.replace(".", "/");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(packagePath);
        if (resource != null) {
            File packageDirectory = new File(resource.getPath());
            if (packageDirectory.exists() && packageDirectory.isDirectory()) {
                File[] classFiles = packageDirectory.listFiles(file -> file.getName().endsWith(".class"));
                if (classFiles != null) {
                    for (File classFile : classFiles) {
                        String className = packageName + "." + classFile.getName().replace(".class", "");
                        try {
                            Class<?> messageClass = Class.forName(className);
                            String simpleName = messageClass.getSimpleName();
                            if (!simpleName.contains("_") || simpleName.contains("$") && simpleName.indexOf("$") < simpleName.indexOf("_")  || simpleName.endsWith("OrBuilder")) {
                                continue;
                            }
                            Method getDefaultInstanceMethod = messageClass.getDeclaredMethod("getDefaultInstance");
                            getDefaultInstanceMethod.setAccessible(true);
                            Object defaultInstance = getDefaultInstanceMethod.invoke(null);
                            if (defaultInstance instanceof MessageLite messageLite) {
                                // 解析协议号
                                Pattern pattern = Pattern.compile(".*_(\\d+)$");
                                Matcher matcher = pattern.matcher(simpleName);
                                if (matcher.matches()) {
                                    int messageType = Integer.parseInt(matcher.group(1));
                                    // 注册消息类型
                                    messageParser.registerMessageType(messageType, messageLite);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        messageParser.messageTypes.forEach((k, v) -> System.out.println(k + ", " + v.getClass().getName()));
    }
    public MyHandler() {
        initParser();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int protocolNumber = msg.readInt();
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        MessageLite message = messageParser.parse(protocolNumber, data);
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(MessageHandler.class)) {
                if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == message.getClass()) {
                    method.invoke(this, message);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + message.getClass());
    }

    @MessageHandler
    public void handleStudent(DataInfo.Student_12345 student) {
        System.out.println("Received student message: " + student);
    }

    @MessageHandler
    public void handleTeacher(DataInfo.Student_23456 msg) {
        System.out.println(msg);
    }
}

