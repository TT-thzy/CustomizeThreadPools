package java.customize.threadPools;


import lombok.extern.slf4j.Slf4j;

import java.customize.util.ThreadUtil;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class BasicThreadPoolTask extends Thread implements ThreadPool {

    private final int initSize;

    private final int maxSize;

    private final int coreSize;

    private int activeCount;

    /**
     * 创建线程所需的工厂
     */
    private final ThreadFactory threadFactory;

    private final RunnableQueue runnableQueue;

    private volatile boolean isShutdown = false;

    /**
     * 工作线程队列 为了add or remove管理使用
     */
    private final Queue<ThreadTaskWrapper> threadTaskWrapperQueue = new ArrayDeque<>();

    private final static DenyPolicy DEFAULT_DENY_POLICY = new DenyPolicy.DiscardDenyPolicy();

    private final static ThreadFactory DEFAULT_THREAD_FACTORY = new DefaultThreadFactory();

    private final long keepAliveTime;

    private final TimeUnit timeUnit;


    public BasicThreadPoolTask(int initSize, int maxSize, int coreSize,
                               int queueSize) {
        this(initSize, maxSize, coreSize, DEFAULT_THREAD_FACTORY,
                queueSize, DEFAULT_DENY_POLICY, 10, TimeUnit.SECONDS);
    }

    public BasicThreadPoolTask(int initSize, int maxSize, int coreSize,
                               ThreadFactory threadFactory, int queueSize,
                               DenyPolicy denyPolicy, long keepAliveTime, TimeUnit timeUnit) {
        this.initSize = initSize;
        this.maxSize = maxSize;
        this.coreSize = coreSize;
        this.threadFactory = threadFactory;
        this.runnableQueue = new LinkedRunnableQueue(queueSize, denyPolicy, this);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.init();
    }

    private void init() {
        // 由于InterruptException是checked exception 所以这里注册并没有效果
        // 需要线程抛出RuntimeException并且basicThreadPoolTask需要获取到内容才需要
        // 如果只是为了日志记录 那么在各个线程中记录也是可以的
        Thread.setDefaultUncaughtExceptionHandler((t,e)->log.error(ThreadUtil.exceptionToString(e)));

        // 将自身线程启动起来
        start();
        // 将initSize线程启动起来 启动后为Runnable状态
        // 在InternalTask#runnableQueue.take()处 变为waiting状态
        for (int i = 0; i < initSize; i++) {
            newThread();
        }
    }

    private void newThread() {
        // 这里是一个关键的代码 来理解InternalTask的作用
        InternalTask internalTask = new InternalTask(runnableQueue);
        // 实际上BasicThreadPoolTask 线程池中 每个线程执行的任务都是InternalTask
        Thread thread = this.threadFactory.createThread(internalTask);
        // 然后把thread和InternalTask 组合成为ThreadTask对象
        ThreadTaskWrapper threadTaskWrapper = new ThreadTaskWrapper(thread, internalTask);
        // 将这个ThreadTask 放到一个队列里面
        threadTaskWrapperQueue.offer(threadTaskWrapper);
        log.warn("basic thread pool create thread:{}", threadTaskWrapper.thread.getName());
        this.activeCount++;
        thread.start();
    }

    private void removeThread() {
        ThreadTaskWrapper threadTaskWrapper = threadTaskWrapperQueue.remove();
        log.warn("basic thread pool remove thread:{}", threadTaskWrapper.thread.getName());
        threadTaskWrapper.internalTask.stop();
        // interrupt 才会把阻塞在take处的线程打断 结束线程
        threadTaskWrapper.thread.interrupt();
        this.activeCount--;
    }

    @Override
    public void run() {
        /**
         * 线程池中线程数量的维护主要由run负责 这也是为什么BasicThreadPoolTask 继承自Thread了
         */
        while (!isShutdown && !isInterrupted()) {
            // sleep的时候是可以被打断的 于是线程就退出了
            try {
                timeUnit.sleep(keepAliveTime);
            } catch (InterruptedException e) {
                log.error(ThreadUtil.exceptionToString(e));
                isShutdown = true;
                break;
            }

            synchronized (this) {
                if (isShutdown) {
                    break;
                }
                log.info("runnableQueue size = {}, activeCount = {}", runnableQueue.getSize(),activeCount);

                // 当前的队列中有任务尚未处理，并且activeCount<coreSize 则继续扩容
                if (runnableQueue.getSize() > 0 && activeCount < coreSize) {
                    for (int i = initSize; i < coreSize; i++) {
                        newThread();
                    }
                    // continue 的目的在于不想让线程的扩容直接达到maxsize
                    continue;
                }

                // 当前的队列中有任务尚未处理，并且activeCount< maxSize 则继续扩容
                if (runnableQueue.getSize() > 0 && activeCount < maxSize) {
                    for (int i = coreSize; i < maxSize; i++) {
                        newThread();
                    }
                }

                // 如果任务队列中没有任务，则需要回收， 回收至coreSize 即可
                if (runnableQueue.getSize() == 0 && activeCount >= maxSize) {
                    for (int i = coreSize; i < maxSize; i++) {
                        removeThread();
                    }
                }
            }
        }
    }

    @Override
    public void execute(Runnable runnable) {
        if (this.isShutdown) {
            throw new IllegalStateException("The thread pool is destroy");
        }
        this.runnableQueue.offer(runnable);
    }

    @Override
    public void shutdown() {
        /**
         * 线程池的销毁同样需要同步机制的保护，主要是为了防止与线程池本身的维护线程引
         * 起数据冲突
         */
        synchronized (this) {
            if (isShutdown) {
                return;
            }
            isShutdown = true;
            // 停止线程池中的活动线程
            threadTaskWrapperQueue.forEach(threadTaskWrapper ->
            {
                threadTaskWrapper.internalTask.stop();
                threadTaskWrapper.thread.interrupt();
            });
            // 我觉得这里是兜底方案 防止isShutdown没有能够停止线程
            this.interrupt();
        }
    }

    @Override
    public int getInitSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destroy");
        }
        return this.initSize;
    }

    @Override
    public int getMaxSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destroy");
        }
        return this.maxSize;
    }

    @Override
    public int getCoreSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destroy");
        }
        return this.coreSize;
    }

    @Override
    public int getQueueSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destroy");
        }
        return runnableQueue.getSize();
    }

    @Override
    public int getActiveCount() {
        synchronized (this) {
            return this.activeCount;
        }
    }

    @Override
    public boolean isShutdown() {
        return this.isShutdown;
    }

    /**
     * InternalTask 和Thread 的一个组合 为了支持这两个操作 需要持有这两个对象
     *                 threadTask.internalTask.stop();
     *                 threadTask.thread.interrupt();
     */
    private static class ThreadTaskWrapper {
        public ThreadTaskWrapper(Thread thread, InternalTask internalTask) {
            this.thread = thread;
            this.internalTask = internalTask;
        }

        Thread thread;

        InternalTask internalTask;
    }

    private static class DefaultThreadFactory implements ThreadFactory {

        private static final AtomicInteger GROUP_COUNTER = new AtomicInteger(1);

        private static final ThreadGroup group = new ThreadGroup("my-thread-pool-group-" + GROUP_COUNTER.getAndIncrement());

        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public Thread createThread(Runnable runnable) {
            return new Thread(group, runnable, "my-thread-pool-exec-" + COUNTER.getAndIncrement());
        }
    }
}
