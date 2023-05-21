package org.netty.firstexample;

import java.util.stream.IntStream;

//设置JVM参数：-Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:InitiatingHeapOccupancyPercent=100
//这样可以保证堆内存总共20m，新生代10m，老年代10m，Eden区8m，Survivor区各1m，老年代占用100%时才触发混合回收
public class EdenFullGC {
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            int size = 2; //初始分配对象的大小为2M
            for (int i = 0; i < 3; i++) {
                byte[] b = new byte[size * 1024 * 1024]; //分配对象
                size *= 2; //每次分配对象的大小翻倍
                if (i == 0 || i == 1) {
                    //第一次和第二次分配对象时，Eden区满了，触发Young GC
                    System.out.println("Young GC");
                } else {
                    //第三次和第四次分配对象时，Eden区无法放下，直接进入老年代，不触发GC
                    System.out.println("No GC");
                }
            }
            Thread.sleep(1000);
        }

    }
}
