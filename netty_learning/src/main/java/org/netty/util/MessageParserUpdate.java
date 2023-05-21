package org.netty.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParserUpdate {

    private final Map<Integer, MessageLite> messageTypes = new ConcurrentHashMap<>();
    private static final String  packageName = "org.netty.protobuf";
    public void registerMessageType(int protocolNumber, MessageLite messageLite) {
        messageTypes.put(protocolNumber, messageLite);
    }

    public MessageParserUpdate() {
        initParser();
    }

    public MessageLite parse(int protocolNumber, byte[] data) throws InvalidProtocolBufferException {
        MessageLite messageLite = messageTypes.get(protocolNumber);
        if (messageLite == null) {
            throw new IllegalArgumentException("Unknown protocol number: " + protocolNumber);
        }
        return messageLite.newBuilderForType().mergeFrom(data).build();
    }

    private void initParser() {
        // 初始化MessageParser对象
        // 遍历指定目录下的所有class文件
        String packagePath = packageName.replace(".", "/");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(packagePath);
        if (resource != null) {
            File packageDirectory = new File(resource.getPath());
            if (packageDirectory.exists() && packageDirectory.isDirectory()) {
                File[] classFiles = packageDirectory.listFiles(file -> file.getName().endsWith(".class"));
                if (classFiles != null) {
                    // 使用并行流来并行处理class文件
                    Arrays.stream(classFiles)
                            .parallel()
                            .forEach(this::processClassFile);
                }
            }
        }
        this.messageTypes.forEach((k, v) -> System.out.println(k + ", " + v.getClass().getName()));
    }

    private void processClassFile(File classFile) {
        String fileName = classFile.getName();
        if (!fileName.endsWith(".class")) {
            return;
        }

        String className = fileName.substring(0, fileName.length() - 6);
        String fullClassName = packageName + "." + className; // 拼接包名和类名
        try {
            Class<?> messageClass = Class.forName(fullClassName);
            String simpleName = messageClass.getSimpleName();
            if (!simpleName.contains("_") || simpleName.contains("$") && simpleName.indexOf("$") < simpleName.indexOf("_") || simpleName.endsWith("OrBuilder")) {
                return;
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
                    this.registerMessageType(messageType, messageLite);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        new MessageParserUpdate();
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        // 计算执行时间
        long executionTime = endTime - startTime;

        // 输出执行时间
        System.out.println("程序执行时间：" + executionTime + " 毫秒");
    }
}
