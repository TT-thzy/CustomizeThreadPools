package java.customize.threadPools;

/**
 * @
 * @date 2020/6/11
 */
public interface ThreadFactory {

    Thread createThread(Runnable runnable);
}
