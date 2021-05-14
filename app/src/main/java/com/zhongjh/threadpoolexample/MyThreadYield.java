package com.zhongjh.threadpoolexample;

/**
 * 礼让线程
 * @author zhongjh
 * @date 2021/5/14
 */
public class MyThreadYield extends Thread {

    public MyThreadYield(String name) {
        super(name);
    }

    @Override
    public synchronized void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(getName() + "在运行，i的值为：" + i + " 优先级为：" + getPriority());
            if (i == 2) {
                System.out.println(getName() + "礼让");
                Thread.yield();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}