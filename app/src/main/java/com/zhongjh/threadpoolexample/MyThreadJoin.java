package com.zhongjh.threadpoolexample;

/**
 * join示例
 * @author zhongjh
 * @date 2021/5/7
 */
public class MyThreadJoin extends Thread {

    int m = (int) (Math.random() * 10000);

    @Override
    public void run() {
        try {
            System.out.println("我在子线程中会随机睡上0-9秒，时间为="+m);
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
