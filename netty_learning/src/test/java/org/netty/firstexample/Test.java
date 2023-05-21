package org.netty.firstexample;

public class Test {
    public static void main(String[] args) {
        A aa = new A();
        new Thread(aa::aa).start();
        new Thread(() -> new B().bb()).start();
        new Thread(aa::bb).start();
        new Thread(A::cc).start();
    }
}
class A {
    synchronized void aa() {
        System.out.println("A");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    synchronized void bb() {
        System.out.println("A bb");
    }
    synchronized static void cc() {
        System.out.println("A static");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
class B {
    synchronized void bb() {
        System.out.println("B");
    }
}
