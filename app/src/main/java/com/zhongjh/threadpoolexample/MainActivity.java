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

import static java.util.concurrent.TimeUnit.SECONDS;

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
            case R.id.btnVolatileNo:
                testVolatileNo();
                break;
            case R.id.btnLinkedBlockingQueueOffer:
                testLinkedBlockingQueueOffer();
                break;
            case R.id.btnSingle:
                testSingle();
                break;
            case R.id.btnCached:
                testCached();
            case R.id.btnIo:
                testIo();
            case R.id.btnCpu:
                testCpu();
            case R.id.btnFixed:
                testFixed();
                break;
            case R.id.btnFixedPeriod:
                testFixedPeriod();
            default:
                break;
        }
    }

    /**
     * ?????????????????????????????????Thread
     */
    private void testThread() {
        MyThread myThread1 = new MyThread("??????1");
        MyThread myThread2 = new MyThread("??????2");
        myThread1.start();
        myThread2.start();
    }

    /**
     * ?????????????????????????????????Runnable
     */
    private void testRunnable() {
        MyThreadImpl myThreadImpl1 = new MyThreadImpl("??????1");
        MyThreadImpl myThreadImpl2 = new MyThreadImpl("??????2");
        Thread myThreadOne = new Thread(myThreadImpl1);
        Thread myThreadTwo = new Thread(myThreadImpl2);
        myThreadOne.start();
        myThreadTwo.start();
    }

    /**
     * ???????????????????????????1????????????????????????
     */
    private void testThreadNewName() {
        MyThreadNewNameImpl myThreadImpl1 = new MyThreadNewNameImpl();
        MyThreadNewNameImpl myThreadImpl2 = new MyThreadNewNameImpl();
        Thread myThreadOne = new Thread(myThreadImpl1, "??????1");
        Thread myThreadTwo = new Thread(myThreadImpl2);
        myThreadOne.start();
        myThreadTwo.start();
    }

    /**
     * join????????????
     */
    private void testJoin() {
        MyThreadJoin myThread = new MyThreadJoin();
        myThread.start();
        try {
            myThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("??????????????????????????????????????????????????????join??????main???????????????????????????????????????????????????");
    }

    /**
     * Sleep??????
     */
    private void testSleep() {
        MyThreadSleep myThread1 = new MyThreadSleep();
        myThread1.start();
    }

    /**
     * Yield??????
     */
    private void testYield() {
        MyThreadYield myThread1 = new MyThreadYield("?????????");
        MyThreadYield myThread2 = new MyThreadYield("?????????");
        myThread1.start();
        myThread2.start();
    }

    private final Object lockObject = new Object();

    /**
     * wait??????
     */
    private void testWait() {
        // ???????????????
        Thread thread = new MyThreadWait(lockObject);
        thread.start();

        long start = System.currentTimeMillis();
        synchronized (lockObject) {
            try {
                System.out.println("lockObject??????");
                lockObject.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("lockObject?????? --> ??????????????????" + (System.currentTimeMillis() - start));
        }
    }

    private int ticket = 10;

    /**
     * Synchronized????????????????????????
     */
    private synchronized void testSynchronized() {
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    // ??????
                    sellTicket();
                }
            }.start();
        }
    }

    /**
     * ??????????????????synchronized
     */
    private void sellTicket() {
        ticket--;
        System.out.println("??????????????????" + ticket);
        if (ticket == 0) {
            // ??????????????????????????????
            ticket = 10;
        }
    }

    /**
     * ????????????????????????????????????
     */
    private void testSynchronized2() {
        final SynchronizedEntity synchronizedEntity = new SynchronizedEntity();

        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity.syncMethod();
            }
        }.start();
        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity.syncThis();
            }
        }.start();
    }

    /**
     * ???????????????????????????????????????
     */
    private void testSynchronized3() {
        final SynchronizedEntity synchronizedEntity1 = new SynchronizedEntity();
        final SynchronizedEntity synchronizedEntity2 = new SynchronizedEntity();

        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity1.syncMethod();
            }
        }.start();
        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedEntity2.syncMethod();
            }
        }.start();
    }

    /**
     * ??????????????????????????????????????????
     */
    private void testSynchronized4() {
        final SynchronizedEntity synchronizedDemo = new SynchronizedEntity();

        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedDemo.syncMethod();
            }
        }.start();

        // ?????????
        new Thread() {
            @Override
            public void run() {
                synchronizedDemo.syncClassMethod();
            }
        }.start();
    }

    /**
     * ?????????
     */
    private final Object lock = new Object();
    /**
     * ??????????????????????????????
     */
    boolean beShouldSub = true;

    /**
     * wait???notify??????
     * ???????????????2???????????????????????????3????????????????????????????????????2???????????????????????????????????????3??????????????????10???
     */
    private void testWaitNotify() {
        // ?????????
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    testWaitNotifyThread();
                }
            }
        }.start();
        // ?????????
        for (int i = 0; i < 10; i++) {
            testWaitNotifyMain();
        }
    }

    /**
     * ?????????????????????
     */
    private void testWaitNotifyThread() {
        synchronized (lock) {
            if (!beShouldSub) {
                // ??????
                try {
                    Log.d("testWaitNotify", "???????????????lock");
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int j = 0; j < 2; j++) {
                Log.d("testWaitNotify", "????????????" + (j + 1) + "???");
            }
            // ????????????????????????????????????????????????false
            beShouldSub = false;
            // ??????
            Log.d("testWaitNotify", "???????????????lock");
            lock.notify();
        }
    }

    /**
     * ???????????????3???
     */
    private void testWaitNotifyMain() {
        synchronized (lock) {
            if (beShouldSub) {
                // ??????
                try {
                    Log.d("testWaitNotify", "???????????????lock");
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int j = 0; j < 3; j++) {
                Log.d("testWaitNotify", "????????????" + (j + 1) + "???");
            }
            // ????????????????????????????????????????????????true
            beShouldSub = true;
            // ??????
            Log.d("testWaitNotify", "???????????????lock");
            lock.notify();
        }
    }

    /**
     * ???????????????
     *
     * @param corePoolSize    ??????????????????
     *                        ???????????????????????????????????????????????????????????????corePoolSize?????????????????????????????????????????????corePoolSize????????????????????????????????????
     *                        ????????????????????????????????????????????????????????????????????????????????????????????????(????????????)???
     *                        ??????????????? allowCoreThreadTimeOut ??? true????????????????????????????????????(????????????)???????????????????????????(????????????????????????)????????????????????????
     * @param maximumPoolSize ??????????????????????????????????????????
     *                        ???????????? = ??????????????? + ?????????????????????
     * @param keepAliveTime   ????????????????????????????????????
     *                        ????????????????????????????????????????????????????????????????????????????????????????????????
     *                        ???????????? allowCoreThreadTimeOut ??? true ???????????????????????????????????????
     * @param unit            keepAliveTime ????????????
     *                        ??????????????????????????????????????????TimeUnit.SECONDS????????????TimeUnit.MILLISECONDS???????????????
     * @param workQueue       ???????????????
     *                        ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????workQueue ?????????
     * @param threadFactory   ???????????????
     *                        ???????????????????????????????????????????????????????????????
     * @param handler         ???????????????
     *                        ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *                        ?????????????????????????????????????????????????????????????????? execute() ????????????????????????????????????
     * @return ThreadPoolExecutor ?????????
     */
    private ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        try {
            return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                    workQueue, threadFactory, handler);
        } catch (Throwable ex) {
            // ??????demo???null?????????????????????????????????????????????
            return null;
        }
    }

    /**
     * ??????Volatile?????????????????????
     * ??????????????????????????????????????????????????????????????????????????????10000????????????
     * ????????????????????????????????????????????????????????????volatile?????????????????????????????????????????????
     * ????????????????????????????????????????????????????????????????????????????????????1???????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ????????????????????????inc?????????10
     * ??????1????????????????????????????????????1??????????????????inc???????????????????????????1????????????
     * ????????????2????????????????????????????????????2??????????????????inc?????????????????????1??????11?????????11???????????????????????????????????????
     * ????????????1???????????????1??????????????????????????????inc??????????????????????????????1??????????????????inc???????????????10???????????????1???inc?????????1?????????inc?????????11????????????11??????????????????????????????????????????
     * ?????????????????????????????????????????????????????????inc????????????1???
     */
    private void testVolatileNo() {
        final Test test = new Test();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        test.increase();
                    }
                    if (finalI == 9) {
                        System.out.println("test.inc: " + test.inc);
                    }
                }
            }.start();
        }
    }

    /**
     * ????????????LinkedBlockingQueue???offer?????????
     */
    private void testLinkedBlockingQueueOffer() {
        LinkedBlockingQueue<String> fruitQueue = new LinkedBlockingQueue<>(2);

        System.out.println(fruitQueue.offer("apple"));
        System.out.println(fruitQueue.offer("orange"));
        System.out.println(fruitQueue.offer("berry"));
        System.out.println(fruitQueue.size());

    }

    /**
     * ????????????ThreadUtils????????????Single
     * Logcat??????TAG???ThreadUtils
     */
    private void testSingle() {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            ThreadUtils.executeByCached(new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            });
        }
    }


    /**
     * ????????????ThreadUtils????????????Cached
     * ????????????????????????????????????60??????????????????????????????????????????
     * Logcat??????TAG???ThreadUtils
     */
    private void testCached() {
        for (int i = 0; i < 128; i++) {
            int finalI = i;
            ThreadUtils.executeByCached(new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            });
        }
    }

    /**
     * ????????????ThreadUtils????????????Io
     * Logcat??????TAG???ThreadUtils
     */
    private void testIo() {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            ThreadUtils.executeByIo(new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            });
        }
    }

    /**
     * ????????????ThreadUtils????????????Cpu
     * Logcat??????TAG???ThreadUtils
     */
    private void testCpu() {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            ThreadUtils.executeByCpu(new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            });
        }
    }

    /**
     * ????????????ThreadUtils????????????Fixed
     * Logcat??????TAG???ThreadUtils
     */
    private void testFixed() {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            ThreadUtils.executeByFixed(10, new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            }, 10);
        }
    }

    /**
     * ????????????ThreadUtils????????????Fixed,????????????10???????????????
     * Logcat??????TAG???ThreadUtils
     */
    private void testFixedPeriod() {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            ThreadUtils.executeByFixedAtFixRate(10, new ThreadUtils.BaseSimpleBaseTask<Object>() {
                @Override
                public Object doInBackground() {
                    return finalI;
                }

                @Override
                public void onSuccess(Object result) {
                    Log.d("testSingle", result + "");
                }
            }, 1, 10, SECONDS);
        }
    }

}