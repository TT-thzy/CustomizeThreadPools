package java.customize.threadPools;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
public interface ThreadFactory {

    Thread createThread(Runnable runnable);
}
