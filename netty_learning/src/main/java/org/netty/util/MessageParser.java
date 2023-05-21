package org.netty.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    public Map<Integer, MessageLite> messageTypes = new HashMap<>();

    public void registerMessageType(int protocolNumber, MessageLite messageLite) {
        messageTypes.put(protocolNumber, messageLite);
    }

    public MessageParser() {
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
                                    this.registerMessageType(messageType, messageLite);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        this.messageTypes.forEach((k, v) -> System.out.println(k + ", " + v.getClass().getName()));
    }
}
