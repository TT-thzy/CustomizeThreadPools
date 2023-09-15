package java.customize.threadPools;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
public interface ThreadPool {

    /**
     * 提交任务到线程池
     * @param runnable
     */
    void execute(Runnable runnable);

    /**
     * 关闭线程池
     */
    void shutdown();

    /**
     * 获取线程池初始值大小
     * @return
     */
    int getInitSize();

    /**
     * 获取线程池最大线程数
     * @return
     */
    int getMaxSize();

    /**
     * 获取线程池核心线程数
     * @return
     */
    int getCoreSize();

    /**
     * 获取线程池中用于缓存任务队列的长度
     * @return
     */
    int getQueueSize();

    /**
     * 获取线程池中活跃线程的数量
     * @return
     */
    int getActiveCount();

    /**
     * 查看线程池是否被shutdown
     * @return
     */
    boolean isShutdown();
}
