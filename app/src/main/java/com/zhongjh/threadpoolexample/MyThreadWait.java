package com.zhongjh.threadpoolexample;

/**
 * 自定义线程类
 *
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThreadWait extends Thread {

    private final Object lockObject;

    public MyThreadWait(Object lockObject) {
        this.lockObject = lockObject;
    }

    @Override
    public void run() {
        synchronized (lockObject) {
            try {
                // 子线程等待了2秒钟后唤醒lockObject锁
                sleep(2000);
                System.out.println("lockObject唤醒");
                lockObject.notifyAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
