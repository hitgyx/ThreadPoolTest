package com.test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void log(String message) {
        String time = LocalTime.now().format(formatter);
        String threadName = Thread.currentThread().getName();
        System.out.printf("[%s] [%s] %s%n", time, threadName, message);
    }
}
