package com.zhongjh.threadpoolexample;

/**
 * 用于区分类锁和对象锁的示例实体
 * @author zhongjh
 * @date 2021/5/8
 */
public class SynchronizedEntity {

    private int ticket = 10;

    /**
     * 同步方法，对象锁
     */
    public synchronized void syncMethod() {
        for (int i = 0; i < 1000; i++) {
            if (ticket > 0) {
                ticket--;
                System.out.println(Thread.currentThread().getName() + "剩余的票数：" + ticket);
            }
        }
    }

    /**
     * 同步块，对象锁
     */
    public void syncThis() {
        synchronized (this) {
            for (int i = 0; i < 1000; i++) {
                if (ticket > 0) {
                    ticket--;
                    System.out.println(Thread.currentThread().getName() + "剩余的票数：" + ticket);
                }
            }
        }
    }

    /**
     * 同步class对象，类锁
     */
    public void syncClassMethod() {
        synchronized (SynchronizedEntity.class) {
            for (int i = 0; i < 50; i++) {
                if (ticket > 0) {
                    ticket--;
                    System.out.println(Thread.currentThread().getName() + "剩余的票数：" + ticket);
                }
            }
        }
    }

    /**
     * 同步静态方法，类锁
     */
    public static synchronized void syncStaticMethod(){
        // 暂不演示该方法
    }

}
