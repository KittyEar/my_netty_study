package org.netty.util;


import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtobufClassScanner {

    public static List<Class<?>> scanProtobufClasses(String packageName) {
        List<Class<?>> protobufClasses = new ArrayList<>();

        String packagePath = packageName.replace(".", "/");
        File packageDirectory = new File(packagePath);

        if (packageDirectory.exists() && packageDirectory.isDirectory()) {
            File[] files = packageDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (file.isFile() && fileName.endsWith(".class")) {
                        String className = packageName + "." + fileName.substring(0, fileName.lastIndexOf("."));
                        try {
                            Class<?> cls = Class.forName(className);
                            if (isProtobufMessage(cls)) {
                                protobufClasses.add(cls);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return protobufClasses;
    }

    private static boolean isProtobufMessage(Class<?> cls) {
        // 检查类是否是 Protobuf 消息类
        return cls != null && cls.getName().endsWith("$Builder");
    }

    private static String extractKeyFromClassName(String className) {
        // 提取类名中的后缀数字作为键
        int lastIndex = className.lastIndexOf("_");
        if (lastIndex >= 0) {
            return className.substring(lastIndex + 1);
        }
        return className;
    }

    public static void main(String[] args) {
        String packageName = "org.netty.protobuf";

        List<Class<?>> protobufClasses = scanProtobufClasses(packageName);

        for (Class<?> cls : protobufClasses) {
            System.out.println("Class: " + cls.getSimpleName());
        }
    }
}