package com.zhongjh.threadpoolexample;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhongjh
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnThread:
                testThread();
                break;
            case R.id.btnRunnable:
                testRunnable();
                break;
            case R.id.btnThreadNewName:
                testThreadNewName();
                break;
            case R.id.btnJoin:
                testJoin();
                break;
            case R.id.btnSleep:
                testSleep();
                break;
            case R.id.btnYield:
                testYield();
                break;
            case R.id.btnWait:
                testWait();
                break;
            case R.id.btnSynchronized:
                testSynchronized();
                break;
            case R.id.btnSynchronized2:
                testSynchronized2();
                break;
            case R.id.btnSynchronized3:
                testSynchronized3();
                break;
            case R.id.btnSynchronized4:
                testSynchronized4();
                break;
            case R.id.btnWaitNotify:
                testWaitNotify();
                break;
            case R.id.btnNewThreadPool:
                newThreadPool(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        null,
                        null,
                        null);
                break;
            default:
                break;
        }
    }

    /**
     * 并行执行多个线程，继承Thread
     */
    private void testThread() {
        MyThread myThread1 = new MyThread("线程1");
        MyThread myThread2 = new MyThread("线程2");
        myThread1.start();
        myThread2.start();
    }

    /**
     * 并行执行多个线程，实现Runnable
     */
    private void testRunnable() {
        MyThreadImpl myThreadImpl1 = new MyThreadImpl("线程1");
        MyThreadImpl myThreadImpl2 = new MyThreadImpl("线程2");
        Thread myThreadOne = new Thread(myThreadImpl1);
        Thread myThreadTwo = new Thread(myThreadImpl2);
        myThreadOne.start();
        myThreadTwo.start();
    }

    /**
     * 并行执行多个线程，1线程自定义了名称
     */
    private void testThreadNewName() {
        MyThreadNewNameImpl myThreadImpl1 = new MyThreadNewNameImpl();
        MyThreadNewNameImpl myThreadImpl2 = new MyThreadNewNameImpl();
        Thread myThreadOne = new Thread(myThreadImpl1, "线程1");
        Thread myThreadTwo = new Thread(myThreadImpl2);
        myThreadOne.start();
        myThreadTwo.start();
    }

    /**
     * join方法示例
     */
    private void testJoin() {
        MyThreadJoin myThread = new MyThreadJoin();
        myThread.start();
        try {
            myThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("正常情况下肯定是我先执行完，但是加入join后，main主线程会等待子线程执行完毕后才执行");
    }

    /**
     * Sleep示例
     */
    private void testSleep() {
        MyThreadSleep myThread1 = new MyThreadSleep();
        myThread1.start();
    }

    /**
     * Yield示例
     */
    private void testYield() {
        MyThreadYield myThread1 = new MyThreadYield("线程一");
        MyThreadYield myThread2 = new MyThreadYield("线程二");
        myThread1.start();
        myThread2.start();
    }

    private final Object lockObject = new Object();

    /**
     * wait示例
     */
    private void testWait() {
        // 创建子线程
        Thread thread = new MyThreadWait(lockObject);
        thread.start();

        long start = System.currentTimeMillis();
        synchronized (lockObject) {
            try {
                System.out.println("lockObject等待");
                lockObject.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("lockObject继续 --> 等待的时间：" + (System.currentTimeMillis() - start));
        }
    }

    private int ticket = 10;

    /**
     * Synchronized购买火车票的示例
     */
    private synchronized void testSynchronized() {
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    // 买票
                    sellTicket();
                }
            }.start();
        }
    }

    /**
     * 减少票，同步synchronized
     */
    private void sellTicket() {
        ticket--;
        System.out.println("剩余的票数：" + ticket);
        if (ticket == 0) {
            // 重新填充票数用于测试
            ticket = 10;
        }
    }

    /**
     * 多个线程调用同一个对象锁
     */
    private void testSynchronized2() {
        final SynchronizedEntity synchronizedEntity = new SynchronizedEntity();

        // 线程一
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity.syncMethod();
            }
        }.start();
        // 线程二
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity.syncThis();
            }
        }.start();
    }

    /**
     * 两个线程分别调用不同对象锁
     */
    private void testSynchronized3() {
        final SynchronizedEntity synchronizedEntity1 = new SynchronizedEntity();
        final SynchronizedEntity synchronizedEntity2 = new SynchronizedEntity();

        // 线程一
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity1.syncMethod();
            }
        }.start();
        // 线程二
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity2.syncMethod();
            }
        }.start();
    }

    /**
     * 两个线程分别调用对象锁、类锁
     */
    private void testSynchronized4() {
        final SynchronizedEntity synchronizedDemo = new SynchronizedEntity();

        // 线程一
        new Thread() {
            @Override
            public void run() {
                synchronizedDemo.syncMethod();
            }
        }.start();

        // 线程二
        new Thread() {
            @Override
            public void run() {
                synchronizedDemo.syncClassMethod();
            }
        }.start();
    }

    /**
     * 锁对象
     */
    private final Object lock = new Object();
    /**
     * 是否执行子线程标志位
     */
    boolean beShouldSub = true;

    /**
     * wait和notify示例
     * 子线程循环2次，接着主线程循环3次，接着又回到子线程循环2次，接着再回到主线程又循环3次，如此循环10次
     */
    private void testWaitNotify() {
        // 子线程
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    testWaitNotifyThread();
                }
            }
        }.start();
        // 主线程
        for (int i = 0; i < 10; i++) {
            testWaitNotifyMain();
        }
    }

    /**
     * 子线程循环两次
     */
    private void testWaitNotifyThread() {
        synchronized (lock) {
            if (!beShouldSub) {
                // 等待
                try {
                    Log.d("testWaitNotify", "子线程等待lock");
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int j = 0; j < 2; j++) {
                Log.d("testWaitNotify", "子循环第" + (j + 1) + "次");
            }
            // 子线程执行完毕，子线程标志位设为false
            beShouldSub = false;
            // 唤醒
            Log.d("testWaitNotify", "子线程唤醒lock");
            lock.notify();
        }
    }

    /**
     * 主线程循环3次
     */
    private void testWaitNotifyMain() {
        synchronized (lock) {
            if (beShouldSub) {
                // 等待
                try {
                    Log.d("testWaitNotify", "主线程等待lock");
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int j = 0; j < 3; j++) {
                Log.d("testWaitNotify", "主循环第" + (j + 1) + "次");
            }
            // 主线程执行完毕，子线程标志位设为true
            beShouldSub = true;
            // 唤醒
            Log.d("testWaitNotify", "主线程唤醒lock");
            lock.notify();
        }
    }

    /**
     * 创建线程池
     *
     * @param corePoolSize    核心线程数：
     *                        线程池新建线程的时候，如果当前线程总数小于corePoolSize，则新建的是核心线程，如果超过corePoolSize，则新建的是非核心线程；
     *                        核心线程默认情况下会一直存活在线程池中，即使这个核心线程啥也不干(闲置状态)；
     *                        如果设置了 allowCoreThreadTimeOut 为 true，那么核心线程如果不干活(闲置状态)的话，超过一定时间(时长下面参数决定)，就会被销毁掉。
     * @param maximumPoolSize 线程池能容纳的最大线程数量：
     *                        线程总数 = 核心线程数 + 非核心线程数。
     * @param keepAliveTime   非核心线程空闲存活时长：
     *                        非核心线程空闲时长超过该时长将会被回收，主要应用在缓存线程池中。
     *                        当设置了 allowCoreThreadTimeOut 为 true 时，对核心线程同样起作用。
     * @param unit            keepAliveTime 的单位：
     *                        它是一个枚举类型，常用的如：TimeUnit.SECONDS（秒）、TimeUnit.MILLISECONDS（毫秒）。
     * @param workQueue       任务队列：
     *                        当所有的核心线程都在干活时，新添加的任务会被添加到这个队列中等待处理，如果队列满了，则新建非核心线程执行任务，常用的workQueue 类型：
     * @param threadFactory   线程工厂：
     *                        用来创建线程池中的线程，通常用默认的即可。
     * @param handler         拒绝策略：
     *                        在线程池已经关闭的情况下和任务太多导致最大线程数和任务队列已经饱和，无法再接收新的任务，
     *                        在上面两种情况下，只要满足其中一种时，在使用 execute() 来提交新的任务时将会拒绝
     * @return ThreadPoolExecutor 线程池
     */
    private ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        try {
            return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                    workQueue, threadFactory, handler);
        } catch (Throwable ex) {
            // 因为demo传null，所以会报错，只是讲解参数意义
            return null;
        }
    }
}