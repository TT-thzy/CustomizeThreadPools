package java.customize.threadPools;

/**
 * @
 * @date 2020/6/11
 */
public interface RunnableQueue {

    void offer(Runnable runnable);

    Runnable take() throws InterruptedException;

    int getSize();
}
