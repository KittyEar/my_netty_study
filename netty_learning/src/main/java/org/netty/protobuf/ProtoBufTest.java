package org.netty.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Scanner;

public class ProtoBufTest {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        DataInfo.Student student = DataInfo.Student.newBuilder()
                .setName("kittyguy")
                .setAge(56)
                .setAddress("hebei")
                .build();

        System.out.println(student);
        byte[] studentBytes = student.toByteArray();
        DataInfo.Student student2 = DataInfo.Student.parseFrom(studentBytes);
        System.out.println(student2);
    }
}
