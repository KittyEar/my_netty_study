package org.netty.firstexample;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.stream.IntStream;

public class AtomicIntegerFieldUpdaterTest {
    public static void main(String[] args) {
        Person person = new Person();
        AtomicIntegerFieldUpdater<Person> updater = AtomicIntegerFieldUpdater.newUpdater(Person.class, "age");
        IntStream.range(0, 10).forEach(i -> new Thread(() -> System.out.println(updater.getAndIncrement(person))).start());
    }
}
class Person {
    volatile int age;
}
