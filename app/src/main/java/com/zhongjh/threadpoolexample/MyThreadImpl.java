package com.zhongjh.threadpoolexample;

/**
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThreadImpl implements Runnable {

    private static final int COUNT = 10;

    /**
     * 线程名称
     */
    private final String threadName;

    public MyThreadImpl(String name) {
        this.threadName = name;
    }

    @Override
    public void run() {
        for (int i = 0; i < COUNT; i++) {
            System.out.println(threadName + ": " + i);
        }
    }

}
