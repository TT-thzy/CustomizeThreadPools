package java.customize.threadPools;

import lombok.extern.slf4j.Slf4j;
import util.ThreadUtil;

/**
 * @
 * @date 2020/6/11
 */
@Slf4j
public class InternalTask implements Runnable {

    /**
     * 用于线程池内部， 该类会使用到RunnableQueue，然后不断地从queue中取出某个runnable，并运行runnable的run 方法
     */

    private final RunnableQueue runnableQueue;

    private volatile boolean running = true;

    public InternalTask(RunnableQueue runnableQueue) {
        this.runnableQueue = runnableQueue;
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 如果不打断 即便running=true 但由于其阻塞在这里 走不到循环那里了 导致线程waiting在这里
                /**
                 * "thread-pool--1" #32 prio=5 os_prio=0 tid=0x0000000020d34800 nid=0x3b18 in Object.wait() [0x00000000254ee000]
                 *    java.lang.Thread.State: WAITING (on object monitor)
                 * 	at java.lang.Object.wait(Native Method)
                 * 	at java.lang.Object.wait(Object.java:502)
                 * 	at wangwenjun.phase1.threadpool.LinkedRunnableQueue.take(LinkedRunnableQueue.java:43)
                 * 	- locked <0x000000076be3ec70> (a java.util.LinkedList)
                 * 	at wangwenjun.phase1.threadpool.InternalTask.run(InternalTask.java:25)
                 * 	at java.lang.Thread.run(Thread.java:748)
                 */
                Runnable task = runnableQueue.take();
                task.run();

            } catch (InterruptedException e) {
                log.error(ThreadUtil.exceptionToString(e));
                running = false;
                break;
            }
        }
    }

    public void stop()
    {
        log.error("{} set running = false", Thread.currentThread());
        this.running = false;
    }
}
