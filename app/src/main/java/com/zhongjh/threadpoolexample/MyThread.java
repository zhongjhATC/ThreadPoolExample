package com.zhongjh.threadpoolexample;

/**
 * 自定义线程类
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThread extends Thread {

    private static final int COUNT = 3;

    /**
     * 线程名称
     */
    private final String threadName;

    public MyThread(String name) {
        this.threadName = name;
    }

    @Override
    public void run() {
        for (int i = 0; i < COUNT; i++) {
            System.out.println(threadName + ": " + i);
        }
    }

}
