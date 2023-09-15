package java.customize.threadPools;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
public interface RunnableQueue {

    void offer(Runnable runnable);

    Runnable take() throws InterruptedException;

    int getSize();
}
