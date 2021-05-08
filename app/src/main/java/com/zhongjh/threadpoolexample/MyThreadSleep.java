package com.zhongjh.threadpoolexample;

/**
 * sleep示例
 *
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThreadSleep extends Thread {
    private static final int COUNT = 3;

    @Override
    public void run() {
        for (int i = 0; i < COUNT; i++) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + ": " + i);
        }
    }
}
