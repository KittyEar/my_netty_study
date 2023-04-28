package org.netty.nioTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        int[] ports = new int[]{5000, 5001, 5002, 5003, 5004};
        Selector selector = Selector.open();//抽象类不能new
        for (int port : ports) {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();//抽象类不能new
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);//调用channel的register方法
            System.out.println("监听端口： " + port);
        }

        while (true) {
            int readyEvent = selector.select();//返回事件就绪的事件数量
            System.out.printf("有 %d 个事件就绪%n", readyEvent);
            Set<SelectionKey> selectionKeys = selector.selectedKeys();//return selected-key set.

            System.out.println("就绪事件如下：");
            selectionKeys.forEach(System.out::println);
            /*
              对集合做了一个浅拷贝，然后使用增强的 for 循环来遍历复制后的集合。当我们找到希望删除的元素时，就可以从原始集合
              中删除它。
              不需要使用迭代器或者手动维护索引变量来遍历集合，也能够很好地避免 ConcurrentModificationException 异常的发生
             */
            for (SelectionKey key : new HashSet<>(selectionKeys)) {
                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);

                    socketChannel.register(selector, SelectionKey.OP_READ);

                    selectionKeys.remove(key);

                    System.out.println("获得客户端连接: " + socketChannel.socket());
                } else if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    int bytesRead = 0;
                    while (true) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                        byteBuffer.clear();
                        int read = socketChannel.read(byteBuffer);//读取客户端消息

                        if (read <= 0) {
                            break;
                        }
                        byteBuffer.flip();
                        socketChannel.write(byteBuffer);//将消息原封不动返回给客户端

                        bytesRead += read;
                    }
                    System.out.println("读取： " + bytesRead + " 个字节，来自于" + socketChannel.socket());
                    selectionKeys.remove(key);
                }
            }
        }
    }
}
class ChatRoom {
    private Map<String, Queue<String>> messageQueue;
    private int maxSize;

    public ChatRoom(int maxSize) {
        this.messageQueue = new LinkedHashMap<>();
        this.maxSize = maxSize;
    }

    public void addMessage(String user, String message) {
        Queue<String> messages = messageQueue.getOrDefault(user, new LinkedList<>());
        messages.add(message);
        messageQueue.put(user, messages);
        int size = messageQueue.values().stream().mapToInt(Queue::size).sum();
        while (size > maxSize) {
            String oldestUser = messageQueue.keySet().iterator().next();
            Queue<String> oldestMessages = messageQueue.get(oldestUser);
            oldestMessages.remove();
            if (oldestMessages.isEmpty()) {
                messageQueue.remove(oldestUser);
            }
            size--;
        }
    }

    public Queue<String> getMessage(String user) {
        return messageQueue.get(user);
    }
    public static void main(String[] args) {

        ChatRoom chatRoom = new ChatRoom(3);
        chatRoom.addMessage("user1", "1");
        chatRoom.addMessage("user2", "2");
        chatRoom.addMessage("user3", "3");
        System.out.println(chatRoom.getMessage("user1"));
        System.out.println(chatRoom.messageQueue);
        chatRoom.addMessage("user2", "4");
        System.out.println(chatRoom.messageQueue);
    }
}



