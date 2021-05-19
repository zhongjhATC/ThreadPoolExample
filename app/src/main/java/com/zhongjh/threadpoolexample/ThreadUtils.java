package com.zhongjh.threadpoolexample;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <pre>
 *     author: Blankj
 *     blog  : http://blankj.com
 *     time  : 2018/05/08
 *     desc  : utils about thread
 *     update: zhongjh 优化代码
 * </pre>
 * @author Blankj
 */
public final class ThreadUtils {

    private static final String TAG = ThreadUtils.class.getSimpleName();

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static final Map<Integer, Map<Integer, ExecutorService>> TYPE_PRIORITY_POOLS = new HashMap<>();

    private static final Map<BaseTask, ExecutorService> TASK_POOL_MAP = new ConcurrentHashMap<>();

    /**
     * 返回的是可用的计算资源，而不是CPU物理核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 可以实现循环或延迟任务的线程池
     */
    private static final ScheduledExecutorService mExecutorService = new ScheduledThreadPoolExecutor(1, (ThreadFactory) Thread::new);

    private static final byte TYPE_SINGLE = -1;
    private static final byte TYPE_CACHED = -2;
    private static final byte TYPE_IO = -4;
    private static final byte TYPE_CPU = -8;

    private static Executor sDeliver;

    /**
     * Return whether the thread is the main thread.
     * 返回该线程是否是主线程。
     *
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * @return 返回ui的handler
     */
    public static Handler getMainHandler() {
        return HANDLER;
    }

    /**
     * 在ui主线程上执行事件
     * @param runnable 事件
     */
    public static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            HANDLER.post(runnable);
        }
    }

    /**
     * 在ui主线程上执行事件
     * @param runnable 事件
     * @param delayMillis Runnable被执行之前的延迟(毫秒)。
     */
    public static void runOnUiThreadDelayed(final Runnable runnable, long delayMillis) {
        HANDLER.postDelayed(runnable, delayMillis);
    }

    /**
     * Return a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.
     *
     * @param size The size of thread in the pool.
     * @return a fixed thread pool
     */
    public static ExecutorService getFixedPool(@IntRange(from = 1) final int size) {
        return getPoolByTypeAndPriority(size);
    }

    /**
     * Return a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.
     *
     * @param size     The size of thread in the pool.
     * @param priority The priority of thread in the poll.
     * @return a fixed thread pool
     */
    public static ExecutorService getFixedPool(@IntRange(from = 1) final int size,
                                               @IntRange(from = 1, to = 10) final int priority) {
        return getPoolByTypeAndPriority(size, priority);
    }

    /**
     * Return a thread pool that uses a single worker thread operating
     * off an unbounded queue, and uses the provided ThreadFactory to
     * create a new thread when needed.
     *
     * @return a single thread pool
     */
    public static ExecutorService getSinglePool() {
        return getPoolByTypeAndPriority(TYPE_SINGLE);
    }

    /**
     * Return a thread pool that uses a single worker thread operating
     * off an unbounded queue, and uses the provided ThreadFactory to
     * create a new thread when needed.
     *
     * @param priority The priority of thread in the poll.
     * @return a single thread pool
     */
    public static ExecutorService getSinglePool(@IntRange(from = 1, to = 10) final int priority) {
        return getPoolByTypeAndPriority(TYPE_SINGLE, priority);
    }

    /**
     * Return a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.
     *
     * @return a cached thread pool
     */
    public static ExecutorService getCachedPool() {
        return getPoolByTypeAndPriority(TYPE_CACHED);
    }

    /**
     * Return a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.
     *
     * @param priority The priority of thread in the poll.
     * @return a cached thread pool
     */
    public static ExecutorService getCachedPool(@IntRange(from = 1, to = 10) final int priority) {
        return getPoolByTypeAndPriority(TYPE_CACHED, priority);
    }

    /**
     * Return a thread pool that creates (2 * CPU_COUNT + 1) threads
     * operating off a queue which size is 128.
     *
     * @return a IO thread pool
     */
    public static ExecutorService getIoPool() {
        return getPoolByTypeAndPriority(TYPE_IO);
    }

    /**
     * Return a thread pool that creates (2 * CPU_COUNT + 1) threads
     * operating off a queue which size is 128.
     *
     * @param priority The priority of thread in the poll.
     * @return a IO thread pool
     */
    public static ExecutorService getIoPool(@IntRange(from = 1, to = 10) final int priority) {
        return getPoolByTypeAndPriority(TYPE_IO, priority);
    }

    /**
     * Return a thread pool that creates (CPU_COUNT + 1) threads
     * operating off a queue which size is 128 and the maximum
     * number of threads equals (2 * CPU_COUNT + 1).
     *
     * @return a cpu thread pool for
     */
    public static ExecutorService getCpuPool() {
        return getPoolByTypeAndPriority(TYPE_CPU);
    }

    /**
     * Return a thread pool that creates (CPU_COUNT + 1) threads
     * operating off a queue which size is 128 and the maximum
     * number of threads equals (2 * CPU_COUNT + 1).
     *
     * @param priority The priority of thread in the poll.
     * @return a cpu thread pool for
     */
    public static ExecutorService getCpuPool(@IntRange(from = 1, to = 10) final int priority) {
        return getPoolByTypeAndPriority(TYPE_CPU, priority);
    }

    /**
     * Executes the given task in a fixed thread pool.
     *
     * @param size The size of thread in the fixed thread pool.
     * @param baseTask The task to execute.
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeByFixed(@IntRange(from = 1) final int size, final BaseTask<T> baseTask) {
        execute(getPoolByTypeAndPriority(size), baseTask);
    }

    /**
     * Executes the given task in a fixed thread pool.
     *
     * @param size     The size of thread in the fixed thread pool.
     * @param baseTask     The task to execute.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByFixed(@IntRange(from = 1) final int size,
                                          final BaseTask<T> baseTask,
                                          @IntRange(from = 1, to = 10) final int priority) {
        execute(getPoolByTypeAndPriority(size, priority), baseTask);
    }

    /**
     * Executes the given task in a fixed thread pool after the given delay.
     *
     * @param size  The size of thread in the fixed thread pool.
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeByFixedWithDelay(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   final long delay,
                                                   final TimeUnit unit) {
        executeWithDelay(getPoolByTypeAndPriority(size), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a fixed thread pool after the given delay.
     *
     * @param size     The size of thread in the fixed thread pool.
     * @param baseTask     The task to execute.
     * @param delay    The time from now to delay execution.
     * @param unit     The time unit of the delay parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByFixedWithDelay(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   final long delay,
                                                   final TimeUnit unit,
                                                   @IntRange(from = 1, to = 10) final int priority) {
        executeWithDelay(getPoolByTypeAndPriority(size, priority), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a fixed thread pool at fix rate.
     *
     * @param size   The size of thread in the fixed thread pool.
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeByFixedAtFixRate(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   final long period,
                                                   final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(size), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a fixed thread pool at fix rate.
     *
     * @param size     The size of thread in the fixed thread pool.
     * @param baseTask     The task to execute.
     * @param period   The period between successive executions.
     * @param unit     The time unit of the period parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByFixedAtFixRate(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   final long period,
                                                   final TimeUnit unit,
                                                   @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(size, priority), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a fixed thread pool at fix rate.
     * 在固定的线程池中以固定的速率煦暖执行给定的任务。
     *
     * @param size         The size of thread in the fixed thread pool.
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions. 周期时间
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByFixedAtFixRate(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   long initialDelay,
                                                   final long period,
                                                   final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(size), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in a fixed thread pool at fix rate.
     *
     * @param size         The size of thread in the fixed thread pool.
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param priority     The priority of thread in the poll.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByFixedAtFixRate(@IntRange(from = 1) final int size,
                                                   final BaseTask<T> baseTask,
                                                   long initialDelay,
                                                   final long period,
                                                   final TimeUnit unit,
                                                   @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(size, priority), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in a single thread pool.
     *
     * @param baseTask The task to execute.
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeBySingle(final BaseTask<T> baseTask) {
        execute(getPoolByTypeAndPriority(TYPE_SINGLE), baseTask);
    }

    /**
     * Executes the given task in a single thread pool.
     *
     * @param baseTask     The task to execute.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeBySingle(final BaseTask<T> baseTask,
                                           @IntRange(from = 1, to = 10) final int priority) {
        execute(getPoolByTypeAndPriority(TYPE_SINGLE, priority), baseTask);
    }

    /**
     * Executes the given task in a single thread pool after the given delay.
     *
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeBySingleWithDelay(final BaseTask<T> baseTask,
                                                    final long delay,
                                                    final TimeUnit unit) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_SINGLE), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a single thread pool after the given delay.
     *
     * @param baseTask     The task to execute.
     * @param delay    The time from now to delay execution.
     * @param unit     The time unit of the delay parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeBySingleWithDelay(final BaseTask<T> baseTask,
                                                    final long delay,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_SINGLE, priority), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a single thread pool at fix rate.
     *
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeBySingleAtFixRate(final BaseTask<T> baseTask,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_SINGLE), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a single thread pool at fix rate.
     *
     * @param baseTask     The task to execute.
     * @param period   The period between successive executions.
     * @param unit     The time unit of the period parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeBySingleAtFixRate(final BaseTask<T> baseTask,
                                                    final long period,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_SINGLE, priority), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a single thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeBySingleAtFixRate(final BaseTask<T> baseTask,
                                                    long initialDelay,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_SINGLE), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in a single thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param priority     The priority of thread in the poll.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeBySingleAtFixRate(final BaseTask<T> baseTask,
                                                    long initialDelay,
                                                    final long period,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(
                getPoolByTypeAndPriority(TYPE_SINGLE, priority), baseTask, initialDelay, period, unit
        );
    }

    /**
     * Executes the given task in a cached thread pool.
     * 在缓存的线程池中执行给定的任务。
     *
     * @param baseTask The task to execute. 要执行的任务。
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeByCached(final BaseTask<T> baseTask) {
        execute(getPoolByTypeAndPriority(TYPE_CACHED), baseTask);
    }

    /**
     * Executes the given task in a cached thread pool.
     *
     * @param baseTask     The task to execute.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCached(final BaseTask<T> baseTask,
                                           @IntRange(from = 1, to = 10) final int priority) {
        execute(getPoolByTypeAndPriority(TYPE_CACHED, priority), baseTask);
    }

    /**
     * Executes the given task in a cached thread pool after the given delay.
     *
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeByCachedWithDelay(final BaseTask<T> baseTask,
                                                    final long delay,
                                                    final TimeUnit unit) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_CACHED), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a cached thread pool after the given delay.
     *
     * @param baseTask     The task to execute.
     * @param delay    The time from now to delay execution.
     * @param unit     The time unit of the delay parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCachedWithDelay(final BaseTask<T> baseTask,
                                                    final long delay,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_CACHED, priority), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a cached thread pool at fix rate.
     *
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeByCachedAtFixRate(final BaseTask<T> baseTask,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CACHED), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a cached thread pool at fix rate.
     *
     * @param baseTask     The task to execute.
     * @param period   The period between successive executions.
     * @param unit     The time unit of the period parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCachedAtFixRate(final BaseTask<T> baseTask,
                                                    final long period,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CACHED, priority), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a cached thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByCachedAtFixRate(final BaseTask<T> baseTask,
                                                    long initialDelay,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CACHED), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in a cached thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param priority     The priority of thread in the poll.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByCachedAtFixRate(final BaseTask<T> baseTask,
                                                    long initialDelay,
                                                    final long period,
                                                    final TimeUnit unit,
                                                    @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(
                getPoolByTypeAndPriority(TYPE_CACHED, priority), baseTask, initialDelay, period, unit
        );
    }

    /**
     * 在IO线程池中执行给定的任务。
     *
     * @param baseTask The task to execute.
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeByIo(final BaseTask<T> baseTask) {
        execute(getPoolByTypeAndPriority(TYPE_IO), baseTask);
    }

    /**
     * Executes the given task in an IO thread pool.
     *
     * @param baseTask     The task to execute.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByIo(final BaseTask<T> baseTask,
                                       @IntRange(from = 1, to = 10) final int priority) {
        execute(getPoolByTypeAndPriority(TYPE_IO, priority), baseTask);
    }

    /**
     * Executes the given task in an IO thread pool after the given delay.
     *
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeByIoWithDelay(final BaseTask<T> baseTask,
                                                final long delay,
                                                final TimeUnit unit) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_IO), baseTask, delay, unit);
    }

    /**
     * Executes the given task in an IO thread pool after the given delay.
     *
     * @param baseTask     The task to execute.
     * @param delay    The time from now to delay execution.
     * @param unit     The time unit of the delay parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByIoWithDelay(final BaseTask<T> baseTask,
                                                final long delay,
                                                final TimeUnit unit,
                                                @IntRange(from = 1, to = 10) final int priority) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_IO, priority), baseTask, delay, unit);
    }

    /**
     * Executes the given task in an IO thread pool at fix rate.
     * 在IO线程池中以固定的速率执行给定的任务。
     *
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeByIoAtFixRate(final BaseTask<T> baseTask,
                                                final long period,
                                                final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_IO), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in an IO thread pool at fix rate.
     *
     * @param baseTask     The task to execute.
     * @param period   The period between successive executions.
     * @param unit     The time unit of the period parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByIoAtFixRate(final BaseTask<T> baseTask,
                                                final long period,
                                                final TimeUnit unit,
                                                @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_IO, priority), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in an IO thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByIoAtFixRate(final BaseTask<T> baseTask,
                                                long initialDelay,
                                                final long period,
                                                final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_IO), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in an IO thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param priority     The priority of thread in the poll.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByIoAtFixRate(final BaseTask<T> baseTask,
                                                long initialDelay,
                                                final long period,
                                                final TimeUnit unit,
                                                @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(
                getPoolByTypeAndPriority(TYPE_IO, priority), baseTask, initialDelay, period, unit
        );
    }

    /**
     * Executes the given task in a cpu thread pool.
     *
     * @param baseTask The task to execute.
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeByCpu(final BaseTask<T> baseTask) {
        execute(getPoolByTypeAndPriority(TYPE_CPU), baseTask);
    }

    /**
     * Executes the given task in a cpu thread pool.
     *
     * @param baseTask     The task to execute.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCpu(final BaseTask<T> baseTask,
                                        @IntRange(from = 1, to = 10) final int priority) {
        execute(getPoolByTypeAndPriority(TYPE_CPU, priority), baseTask);
    }

    /**
     * Executes the given task in a cpu thread pool after the given delay.
     *
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeByCpuWithDelay(final BaseTask<T> baseTask,
                                                 final long delay,
                                                 final TimeUnit unit) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_CPU), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a cpu thread pool after the given delay.
     *
     * @param baseTask     The task to execute.
     * @param delay    The time from now to delay execution.
     * @param unit     The time unit of the delay parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCpuWithDelay(final BaseTask<T> baseTask,
                                                 final long delay,
                                                 final TimeUnit unit,
                                                 @IntRange(from = 1, to = 10) final int priority) {
        executeWithDelay(getPoolByTypeAndPriority(TYPE_CPU, priority), baseTask, delay, unit);
    }

    /**
     * Executes the given task in a cpu thread pool at fix rate.
     *
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeByCpuAtFixRate(final BaseTask<T> baseTask,
                                                 final long period,
                                                 final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CPU), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a cpu thread pool at fix rate.
     *
     * @param baseTask     The task to execute.
     * @param period   The period between successive executions.
     * @param unit     The time unit of the period parameter.
     * @param priority The priority of thread in the poll.
     * @param <T>      The type of the task's result.
     */
    public static <T> void executeByCpuAtFixRate(final BaseTask<T> baseTask,
                                                 final long period,
                                                 final TimeUnit unit,
                                                 @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CPU, priority), baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a cpu thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByCpuAtFixRate(final BaseTask<T> baseTask,
                                                 long initialDelay,
                                                 final long period,
                                                 final TimeUnit unit) {
        executeAtFixedRate(getPoolByTypeAndPriority(TYPE_CPU), baseTask, initialDelay, period, unit);
    }

    /**
     * Executes the given task in a cpu thread pool at fix rate.
     *
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param priority     The priority of thread in the poll.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByCpuAtFixRate(final BaseTask<T> baseTask,
                                                 long initialDelay,
                                                 final long period,
                                                 final TimeUnit unit,
                                                 @IntRange(from = 1, to = 10) final int priority) {
        executeAtFixedRate(
                getPoolByTypeAndPriority(TYPE_CPU, priority), baseTask, initialDelay, period, unit
        );
    }

    /**
     * Executes the given task in a custom thread pool.
     *
     * @param pool The custom thread pool.
     * @param baseTask The task to execute.
     * @param <T>  The type of the task's result.
     */
    public static <T> void executeByCustom(final ExecutorService pool, final BaseTask<T> baseTask) {
        execute(pool, baseTask);
    }

    /**
     * Executes the given task in a custom thread pool after the given delay.
     *
     * @param pool  The custom thread pool.
     * @param baseTask  The task to execute.
     * @param delay The time from now to delay execution.
     * @param unit  The time unit of the delay parameter.
     * @param <T>   The type of the task's result.
     */
    public static <T> void executeByCustomWithDelay(final ExecutorService pool,
                                                    final BaseTask<T> baseTask,
                                                    final long delay,
                                                    final TimeUnit unit) {
        executeWithDelay(pool, baseTask, delay, unit);
    }

    /**
     * Executes the given task in a custom thread pool at fix rate.
     *
     * @param pool   The custom thread pool.
     * @param baseTask   The task to execute.
     * @param period The period between successive executions.
     * @param unit   The time unit of the period parameter.
     * @param <T>    The type of the task's result.
     */
    public static <T> void executeByCustomAtFixRate(final ExecutorService pool,
                                                    final BaseTask<T> baseTask,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(pool, baseTask, 0, period, unit);
    }

    /**
     * Executes the given task in a custom thread pool at fix rate.
     *
     * @param pool         The custom thread pool.
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param <T>          The type of the task's result.
     */
    public static <T> void executeByCustomAtFixRate(final ExecutorService pool,
                                                    final BaseTask<T> baseTask,
                                                    long initialDelay,
                                                    final long period,
                                                    final TimeUnit unit) {
        executeAtFixedRate(pool, baseTask, initialDelay, period, unit);
    }

    /**
     * Cancel the given task.
     *
     * @param baseTask The task to cancel.
     */
    public static void cancel(final BaseTask baseTask) {
        if (baseTask == null) {
            return;
        }
        baseTask.cancel();
    }

    /**
     * Cancel the given tasks.
     *
     * @param baseTasks The tasks to cancel.
     */
    public static void cancel(final BaseTask... baseTasks) {
        if (baseTasks == null || baseTasks.length == 0) {
            return;
        }
        for (BaseTask baseTask : baseTasks) {
            if (baseTask == null) {
                continue;
            }
            baseTask.cancel();
        }
    }

    /**
     * Cancel the given tasks.
     *
     * @param baseTasks The tasks to cancel.
     */
    public static void cancel(final List<BaseTask> baseTasks) {
        if (baseTasks == null || baseTasks.size() == 0) {
            return;
        }
        for (BaseTask baseTask : baseTasks) {
            if (baseTask == null) {
                continue;
            }
            baseTask.cancel();
        }
    }

    /**
     * Cancel the tasks in pool.
     *
     * @param executorService The pool.
     */
    public static void cancel(ExecutorService executorService) {
        if (executorService instanceof ThreadPoolExecutor4Util) {
            for (Map.Entry<BaseTask, ExecutorService> taskTaskInfoEntry : TASK_POOL_MAP.entrySet()) {
                if (taskTaskInfoEntry.getValue() == executorService) {
                    cancel(taskTaskInfoEntry.getKey());
                }
            }
        } else {
            Log.e("ThreadUtils", "The executorService is not ThreadUtils's pool.");
        }
    }

    /**
     * Set the deliver.
     *
     * @param deliver The deliver.
     */
    public static void setDeliver(final Executor deliver) {
        sDeliver = deliver;
    }

    private static <T> void execute(final ExecutorService pool, final BaseTask<T> baseTask) {
        execute(pool, baseTask, 0, 0, null);
    }

    private static <T> void executeWithDelay(final ExecutorService pool,
                                             final BaseTask<T> baseTask,
                                             final long delay,
                                             final TimeUnit unit) {
        execute(pool, baseTask, delay, 0, unit);
    }

    private static <T> void executeAtFixedRate(final ExecutorService pool,
                                               final BaseTask<T> baseTask,
                                               long delay,
                                               final long period,
                                               final TimeUnit unit) {
        execute(pool, baseTask, delay, period, unit);
    }

    /**
     *
     * @param pool ExecutorService 线程池
     */
    private static <T> void execute(final ExecutorService pool, final BaseTask<T> baseTask,
                                    long delay, final long period, final TimeUnit unit) {
        synchronized (TASK_POOL_MAP) {
            if (TASK_POOL_MAP.get(baseTask) != null) {
                Log.e("ThreadUtils", "Task can only be executed once.");
                return;
            }
            TASK_POOL_MAP.put(baseTask, pool);
        }
        if (period == 0) {
            if (delay == 0) {
                pool.execute(baseTask);
            } else {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        pool.execute(baseTask);
                    }
                };
                mExecutorService.schedule(timerTask, unit.toMillis(delay), TimeUnit.MILLISECONDS);
            }
        } else {
            baseTask.setSchedule(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    pool.execute(baseTask);
                }
            };
            mExecutorService.scheduleAtFixedRate(timerTask, unit.toMillis(delay), unit.toMillis(period), TimeUnit.MILLISECONDS);
        }
    }

    private static ExecutorService getPoolByTypeAndPriority(final int type) {
        return getPoolByTypeAndPriority(type, Thread.NORM_PRIORITY);
    }

    /**
     * 根据类型获取线程池
     * @param type 类型
     * @param priority 优先级
     * @return 线程池
     */
    private static ExecutorService getPoolByTypeAndPriority(final int type, final int priority) {
        // 同步map安全，防止线程不安全创建多个 Map线程池
        synchronized (TYPE_PRIORITY_POOLS) {
            ExecutorService pool;
            // 通过类型获取 Map线程池
            Map<Integer, ExecutorService> priorityPools = TYPE_PRIORITY_POOLS.get(type);
            if (priorityPools == null) {
                // 如果没有 Map线程池 则新建一个
                priorityPools = new ConcurrentHashMap<>();
                pool = ThreadPoolExecutor4Util.createPool(type, priority);
                // 加入线程池
                priorityPools.put(priority, pool);
                // 新建后加入 Map线程池
                TYPE_PRIORITY_POOLS.put(type, priorityPools);
            } else {
                // 根据线程池优先级获取线程池
                pool = priorityPools.get(priority);
                // 如果没有该线程池，则创建新的线程池
                if (pool == null) {
                    pool = ThreadPoolExecutor4Util.createPool(type, priority);
                    priorityPools.put(priority, pool);
                }
            }
            return pool;
        }
    }

    /**
     * 继承于ThreadPoolExecutor
     */
    static final class ThreadPoolExecutor4Util extends ThreadPoolExecutor {

        /**
         * 创建线程池
         * @param type 类型
         * @param priority 优先级
         * @return 线程池
         */
        private static ExecutorService createPool(final int type, final int priority) {
            switch (type) {
                case TYPE_SINGLE:
                    // 创建 核心线程数为1，线程池最大线程数量为1，非核心线程空闲存活时长为0
                    // 只创建一个线程确保 顺序执行的场景，并且只有一个线程在执行
                    return new ThreadPoolExecutor4Util(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("single", priority)
                    );
                case TYPE_CACHED:
                    // 创建 核心线程数为0，线程池最大线程数量为128，非核心线程空闲存活时长为60秒
                    // 线程数为128个一般用于处理执行时间比较短的任务
                    return new ThreadPoolExecutor4Util(0, 128,
                            60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cached", priority)
                    );
                case TYPE_IO:
                    // 创建 核心线程数为可计算资源*2+1,线程池最大线程数量为可计算资源*2+1，非核心线程空闲存活时长为30秒
                    return new ThreadPoolExecutor4Util(2 * CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("io", priority)
                    );
                case TYPE_CPU:
                    // 创建 核心线程数为可计算资源+1,线程池最大线程数量为可计算资源*2+1，非核心线程空闲存活时长为30秒
                    return new ThreadPoolExecutor4Util(CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cpu", priority)
                    );
                default:
                    // 创建 核心线程数、线程池最大数量为自定义的，空闲存活时长为0
                    return new ThreadPoolExecutor4Util(type, type,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("fixed(" + type + ")", priority)
                    );
            }
        }

        /**
         * 创建一个原子类
         */
        private final AtomicInteger mSubmittedCount = new AtomicInteger();

        private LinkedBlockingQueue4Util mWorkQueue;

        ThreadPoolExecutor4Util(int corePoolSize, int maximumPoolSize,
                                long keepAliveTime, TimeUnit unit,
                                LinkedBlockingQueue4Util workQueue,
                                ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize,
                    keepAliveTime, unit,
                    workQueue,
                    threadFactory
            );
            workQueue.mPool = this;
            mWorkQueue = workQueue;
        }

        private int getSubmittedCount() {
            return mSubmittedCount.get();
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            // 先自减再获取减1后的值
            mSubmittedCount.decrementAndGet();
            super.afterExecute(r, t);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (this.isShutdown()) {
                return;
            }
            // 先自增再获取加1后的值
            mSubmittedCount.incrementAndGet();
            try {
                super.execute(command);
            } catch (RejectedExecutionException ignore) {
                Log.e(TAG, "This will not happen!");
                mWorkQueue.offer(command);
            } catch (Throwable t) {
                mSubmittedCount.decrementAndGet();
            }
        }
    }

    /**
     * 任务队列类
     * LinkedBlockingQueue这个队列接收到任务的时候，如果当前线程数小于核心线程数，则新建线程(核心线程)处理任务；
     * 如果当前线程数等于核心线程数，则进入队列等待。
     * 由于这个队列没有最大值限制，即所有超过核心线程数的任务都将被添加到队列中，
     * 这也就导致了 maximumPoolSize 的设定失效，因为总线程数永远不会超过 corePoolSize
     *
     * size()是线程队伍排列的总数
     *
     * TYPE_SINGLE：
     * 该类型的线程池mPool一直只有一条线程，并且size会自动随着队列增长而增长，没有最大值的限制。offer一直为true
     * TYPE_CACHED：
     * 该类型会先给线程池添加线程，总数128，这时候offer一直返回false,当线程数达到128条后，就会加入线程队伍队列，此时offer返回true
     * 当60秒闲置时间过了以后，线程池mPool会慢慢清空线程，此时再次调用，就会重新添加线程到线程池，重走上面的步骤
     * TYPE_IO:
     * 涉及到网络、磁盘IO的任务都是IO密集型任务
     * 核心线程和总线程是根据cpu计算的，是cpu的2倍+1，并且队列size会自动随着队列增长而增长，没有最大值的限制。offer一直为true
     * TYPE_CPU:
     * 核心线程和总线程是根据cpu计算的，总线程是cpu的数量+1，总线程是cpu的2倍+1
     * 并且队列size会自动随着队列增长而增长，没有最大值的限制。当线程没加入队列前，offer一直为true
     * 其他，例如Fixed：
     * 核心线程和总线程是自定义的，并且size会自动随着队列增长而增长，没有最大值的限制。offer一直为true
     *
     */
    private static final class LinkedBlockingQueue4Util extends LinkedBlockingQueue<Runnable> {

        /**
         * 线程池
         */
        private volatile ThreadPoolExecutor4Util mPool;

        private int mCapacity = Integer.MAX_VALUE;

        LinkedBlockingQueue4Util() {
            super();
        }

        LinkedBlockingQueue4Util(boolean isAddSubThreadFirstThenAddQueue) {
            super();
            if (isAddSubThreadFirstThenAddQueue) {
                mCapacity = 0;
            }
        }

        LinkedBlockingQueue4Util(int capacity) {
            super();
            mCapacity = capacity;
        }

        @Override
        public boolean offer(@NonNull Runnable runnable) {
            Log.d(TAG,"offer mCapacity:" + mCapacity + "size():" + size() + "mPool:" + (mPool != null ? mPool.getPoolSize() : "null"));
            boolean isOffer;
            // 如果线程数最大值 小于等于 当前线程数 并且 线程池不为空 并且 线程池的线程总数小于线程池的线程总数值
            if (mCapacity <= size() &&
                    mPool != null && mPool.getPoolSize() < mPool.getMaximumPoolSize()) {
                // 返回false表示不加入队列
                isOffer = false;
                if (!isOffer) {
                    Log.d(TAG, "isOffer:" + isOffer);
                }
                return isOffer;
            }
            isOffer = super.offer(runnable);
            if (!isOffer) {
                Log.d(TAG, "isOffer:" + isOffer);
            }
            return isOffer;
        }
    }

    /**
     * 线程工厂类
     */
    static final class UtilsThreadFactory extends AtomicLong
            implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private static final long serialVersionUID = -9209200509960368598L;
        private final String namePrefix;
        private final int priority;
        private final boolean isDaemon;

        UtilsThreadFactory(String prefix, int priority) {
            this(prefix, priority, false);
        }

        UtilsThreadFactory(String prefix, int priority, boolean isDaemon) {
            namePrefix = prefix + "-pool-" +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
            this.priority = priority;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, namePrefix + getAndIncrement()) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        Log.e("ThreadUtils", "Request threw uncaught throwable", t);
                    }
                }
            };
            t.setDaemon(isDaemon);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.out.println(e);
                }
            });
            t.setPriority(priority);
            return t;
        }
    }

    public abstract static class BaseSimpleBaseTask<T> extends BaseTask<T> {

        @Override
        public void onCancel() {
            Log.e("ThreadUtils", "onCancel: " + Thread.currentThread());
        }

        @Override
        public void onFail(Throwable t) {
            Log.e("ThreadUtils", "onFail: ", t);
        }

    }

    public abstract static class BaseTask<T> implements Runnable {

        private static final int NEW = 0;
        private static final int RUNNING = 1;
        private static final int EXCEPTIONAL = 2;
        private static final int COMPLETING = 3;
        private static final int CANCELLED = 4;
        private static final int INTERRUPTED = 5;
        private static final int TIMEOUT = 6;

        /**
         * 原子类的状态
         */
        private final AtomicInteger state = new AtomicInteger(NEW);

        private volatile boolean isSchedule;
        /**
         * 共享变量 线程
         */
        private volatile Thread runner;

        /**
         * 可以实现循环或延迟任务的线程池
         */
        private ScheduledExecutorService mExecutorService;
        private long mTimeoutMillis;
        private OnTimeoutListener mTimeoutListener;

        private Executor deliver;

        /**
         * 线程方法
         * @return 实体
         * @throws Throwable 异常
         */
        public abstract T doInBackground() throws Throwable;

        /**
         * 成功
         * @param result 实体
         */
        public abstract void onSuccess(T result);

        /**
         * 取消
         */
        public abstract void onCancel();

        /**
         * 失败
         * @param t 异常
         */
        public abstract void onFail(Throwable t);

        @Override
        public void run() {
            // 判断是否循环计划内的
            if (isSchedule) {
                // 因为如果是在循环内的，那么runner还是之前的
                if (runner == null) {
                    // 判断当前状态如果是New，便赋值state=RUNNING，如果不是New，便返回
                    if (!state.compareAndSet(NEW, RUNNING)) {
                        return;
                    }
                    // 获取当前线程
                    runner = Thread.currentThread();
                    if (mTimeoutListener != null) {
                        Log.w("ThreadUtils", "Scheduled task doesn't support timeout.");
                    }
                } else {
                    // 如果不是RUNNING便直接返回
                    if (state.get() != RUNNING) {
                        return;
                    }
                }
            } else {
                // 判断当前状态如果是New，便赋值state=RUNNING，如果不是New，便返回
                if (!state.compareAndSet(NEW, RUNNING)) {
                    return;
                }
                // 获取当前线程
                runner = Thread.currentThread();
                if (mTimeoutListener != null) {
                    // 实例化 循环或延迟任务的线程池
                    mExecutorService = new ScheduledThreadPoolExecutor(1, (ThreadFactory) Thread::new);
                    // 调用了延迟运行任务
                    mExecutorService.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isDone() && mTimeoutListener != null) {
                                timeout();
                                mTimeoutListener.onTimeout();
                            }
                        }
                    }, mTimeoutMillis, TimeUnit.MILLISECONDS);
                }
            }
            try {
                // 执行doInBackground方法获取值
                final T result = doInBackground();
                // 判断是否循环计划内的
                if (isSchedule) {
                    // 如果不是RUNNING便直接返回
                    if (state.get() != RUNNING) {
                        return;
                    }
                    getDeliver().execute(() -> onSuccess(result));
                } else {
                    // 判断当前状态如果是RUNNING，便赋值state=COMPLETING，如果不是RUNNING，便返回
                    if (!state.compareAndSet(RUNNING, COMPLETING)) {
                        return;
                    }
                    // 执行成功方法，getDeliver()已经封装了跳转ui线程
                    getDeliver().execute(() -> {
                        onSuccess(result);
                        onDone();
                    });
                }
            } catch (InterruptedException ignore) {
                // 被中断了，判断当前状态如果是CANCELLED，便赋值state=INTERRUPTED
                state.compareAndSet(CANCELLED, INTERRUPTED);
            } catch (final Throwable throwable) {
                // 如果出现异常了，判断当前状态如果是RUNNING，便赋值EXCEPTIONAL
                if (!state.compareAndSet(RUNNING, EXCEPTIONAL)) {
                    return;
                }
                // 执行成功方法，getDeliver()已经封装了跳转ui线程
                getDeliver().execute(() -> {
                    onFail(throwable);
                    onDone();
                });
            }
        }

        public void cancel() {
            cancel(true);
        }

        public void cancel(boolean mayInterruptIfRunning) {
            synchronized (state) {
                if (state.get() > RUNNING) {
                    return;
                }
                state.set(CANCELLED);
            }
            if (mayInterruptIfRunning) {
                if (runner != null) {
                    runner.interrupt();
                }
            }

            getDeliver().execute(() -> {
                onCancel();
                onDone();
            });
        }

        private void timeout() {
            synchronized (state) {
                if (state.get() > RUNNING) {
                    return;
                }
                state.set(TIMEOUT);
            }
            if (runner != null) {
                runner.interrupt();
            }
            onDone();
        }


        public boolean isCanceled() {
            return state.get() >= CANCELLED;
        }

        public boolean isDone() {
            return state.get() > RUNNING;
        }

        public BaseTask<T> setDeliver(Executor deliver) {
            this.deliver = deliver;
            return this;
        }

        /**
         * Scheduled task doesn't support timeout.
         */
        public BaseTask<T> setTimeout(final long timeoutMillis, final OnTimeoutListener listener) {
            mTimeoutMillis = timeoutMillis;
            mTimeoutListener = listener;
            return this;
        }

        private void setSchedule(boolean isSchedule) {
            this.isSchedule = isSchedule;
        }

        private Executor getDeliver() {
            if (deliver == null) {
                return getGlobalDeliver();
            }
            return deliver;
        }

        @CallSuper
        protected void onDone() {
            TASK_POOL_MAP.remove(this);
            if (mExecutorService != null) {
                mExecutorService.shutdownNow();
                mExecutorService = null;
                mTimeoutListener = null;
            }
        }

        public interface OnTimeoutListener {
            /**
             * 超时
             */
            void onTimeout();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj);
        }
    }

    public static class SyncValue<T> {

        private CountDownLatch mLatch = new CountDownLatch(1);
        private AtomicBoolean mFlag = new AtomicBoolean();
        private T mValue;

        public void setValue(T value) {
            if (mFlag.compareAndSet(false, true)) {
                mValue = value;
                mLatch.countDown();
            }
        }

        public T getValue() {
            if (!mFlag.get()) {
                try {
                    mLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return mValue;
        }
    }

    private static Executor getGlobalDeliver() {
        if (sDeliver == null) {
            sDeliver = new Executor() {
                @Override
                public void execute(@NonNull Runnable command) {
                    runOnUiThread(command);
                }
            };
        }
        return sDeliver;
    }
}
