package com.zhongjh.threadpoolexample;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

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

    private final Object lock = new Object();

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
                    synchronized (lock) {
                        for (int j = 0; j < 2; j++) {
                            System.out.println("子循环第" + (j + 1) + "次");
                        }
                        // 唤醒
                        System.out.println("lock子唤醒");
                        lock.notify();
                        // 等待
                        try {
                            System.out.println("lock子等待");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
        // 主线程
        for (int i = 0; i < 10; i++) {
            synchronized (lock) {
                // 等待
                try {
                    System.out.println("lock主等待");
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < 3; j++) {
                    System.out.println("主循环第" + (j + 1) + "次");
                }
                // 唤醒
                System.out.println("lock主唤醒");
                lock.notify();
            }
        }
    }

}