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

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static final Map<Integer, Map<Integer, ExecutorService>> TYPE_PRIORITY_POOLS = new HashMap<>();

    private static final Map<BaseTask, ExecutorService> TASK_POOL_MAP = new ConcurrentHashMap<>();

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
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

    public static Handler getMainHandler() {
        return HANDLER;
    }

    public static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            HANDLER.post(runnable);
        }
    }

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
     *
     * @param size         The size of thread in the fixed thread pool.
     * @param baseTask         The task to execute.
     * @param initialDelay The time to delay first execution.
     * @param period       The period between successive executions.
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
     *
     * @param baseTask The task to execute.
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

    private static ExecutorService getPoolByTypeAndPriority(final int type, final int priority) {
        synchronized (TYPE_PRIORITY_POOLS) {
            ExecutorService pool;
            Map<Integer, ExecutorService> priorityPools = TYPE_PRIORITY_POOLS.get(type);
            if (priorityPools == null) {
                priorityPools = new ConcurrentHashMap<>();
                pool = ThreadPoolExecutor4Util.createPool(type, priority);
                priorityPools.put(priority, pool);
                TYPE_PRIORITY_POOLS.put(type, priorityPools);
            } else {
                pool = priorityPools.get(priority);
                if (pool == null) {
                    pool = ThreadPoolExecutor4Util.createPool(type, priority);
                    priorityPools.put(priority, pool);
                }
            }
            return pool;
        }
    }

    static final class ThreadPoolExecutor4Util extends ThreadPoolExecutor {

        private static ExecutorService createPool(final int type, final int priority) {
            switch (type) {
                case TYPE_SINGLE:
                    return new ThreadPoolExecutor4Util(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("single", priority)
                    );
                case TYPE_CACHED:
                    return new ThreadPoolExecutor4Util(0, 128,
                            60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cached", priority)
                    );
                case TYPE_IO:
                    return new ThreadPoolExecutor4Util(2 * CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("io", priority)
                    );
                case TYPE_CPU:
                    return new ThreadPoolExecutor4Util(CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cpu", priority)
                    );
                default:
                    return new ThreadPoolExecutor4Util(type, type,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("fixed(" + type + ")", priority)
                    );
            }
        }

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
            mSubmittedCount.decrementAndGet();
            super.afterExecute(r, t);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (this.isShutdown()) {
                return;
            }
            mSubmittedCount.incrementAndGet();
            try {
                super.execute(command);
            } catch (RejectedExecutionException ignore) {
                Log.e("ThreadUtils", "This will not happen!");
                mWorkQueue.offer(command);
            } catch (Throwable t) {
                mSubmittedCount.decrementAndGet();
            }
        }
    }

    private static final class LinkedBlockingQueue4Util extends LinkedBlockingQueue<Runnable> {

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
            if (mCapacity <= size() &&
                    mPool != null && mPool.getPoolSize() < mPool.getMaximumPoolSize()) {
                // create a non-core thread
                return false;
            }
            return super.offer(runnable);
        }
    }

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

        private final AtomicInteger state = new AtomicInteger(NEW);

        private volatile boolean isSchedule;
        private volatile Thread runner;

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
            if (isSchedule) {
                if (runner == null) {
                    if (!state.compareAndSet(NEW, RUNNING)) {
                        return;
                    }
                    runner = Thread.currentThread();
                    if (mTimeoutListener != null) {
                        Log.w("ThreadUtils", "Scheduled task doesn't support timeout.");
                    }
                } else {
                    if (state.get() != RUNNING) {
                        return;
                    }
                }
            } else {
                if (!state.compareAndSet(NEW, RUNNING)) {
                    return;
                }
                runner = Thread.currentThread();
                if (mTimeoutListener != null) {
                    mExecutorService = new ScheduledThreadPoolExecutor(1, (ThreadFactory) Thread::new);
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
                final T result = doInBackground();
                if (isSchedule) {
                    if (state.get() != RUNNING) {
                        return;
                    }
                    getDeliver().execute(() -> onSuccess(result));
                } else {
                    if (!state.compareAndSet(RUNNING, COMPLETING)) {
                        return;
                    }
                    getDeliver().execute(() -> {
                        onSuccess(result);
                        onDone();
                    });
                }
            } catch (InterruptedException ignore) {
                state.compareAndSet(CANCELLED, INTERRUPTED);
            } catch (final Throwable throwable) {
                if (!state.compareAndSet(RUNNING, EXCEPTIONAL)) {
                    return;
                }
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
