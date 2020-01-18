package com.qgailab.raftkv.support;

import java.util.concurrent.*;

/**
 * @author linxu
 * @date 2019/11/16
 * <tip>take care of yourself.everything is no in vain.</tip>
 */
public class RaftThreadPool {
    /**
     * 智能CPU片利用
     */
    private static int cpu = Runtime.getRuntime().availableProcessors();

    private static int maxPoolSize = cpu << 1;
    private static final int QUEUE_SIZE = 1024;
    private static final long KEEP_TIME = 1000 * 60;
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;

    private static ScheduledExecutorService ss = getScheduled();
    private static ThreadPoolExecutor te = getThreadPool();

    private static ThreadPoolExecutor getThreadPool() {
        return new RaftThreadPoolExecutor(
                cpu,
                maxPoolSize,
                KEEP_TIME,
                keepTimeUnit,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new NameThreadFactory());
    }

    private static ScheduledExecutorService getScheduled() {
        return new ScheduledThreadPoolExecutor(cpu, new NameThreadFactory());
    }


    public static void scheduleAtFixedRate(Runnable r, long initDelay, long delay) {
        ss.scheduleAtFixedRate(r, initDelay, delay, TimeUnit.MILLISECONDS);
    }


    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        ss.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 添加了异步支持
     *
     * @param r
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Future<T> submit(Callable r) {
        return te.submit(r);
    }

    /**
     * 异步忽略执行
     *
     * @param r
     */
    public static void execute(Runnable r) {
        te.execute(r);
    }

    /**
     * 由调用线程直接调用run方法同步执行
     *
     * @param r
     * @param sync
     */
    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            te.execute(r);
        }
    }

    static class NameThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new RaftThread("Raft thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }

}
