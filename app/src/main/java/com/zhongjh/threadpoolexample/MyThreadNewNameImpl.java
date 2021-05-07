package com.zhongjh.threadpoolexample;

/**
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThreadNewNameImpl implements Runnable {

    private static final int COUNT = 3;

    @Override
    public void run() {
        for (int i = 0; i < COUNT; i++) {
            System.out.println(Thread.currentThread().getName() + ": " + i);
        }
    }

}
