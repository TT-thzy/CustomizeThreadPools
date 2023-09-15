package java.customize.threadPools;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
@Slf4j
public class BasicThreadPoolTest {

    @Test
    @DisplayName("test")
    public void test() throws InterruptedException {

        TimeUnit.SECONDS.sleep(60);

        // 当线程池刚刚创建 还没有Task到来的时候，池中的线程处于什么状态？
        final ThreadPool threadPool = new BasicThreadPoolTask(2, 6, 4, 1000);

        TimeUnit.SECONDS.sleep(60);
        for (int i = 0; i < 20; i++) {
            threadPool.execute(() ->
            {
                try {
                    TimeUnit.SECONDS.sleep(5);
                    log.info(Thread.currentThread().getName() + " is running and done.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        int count = 0;

        for (; ; ) {
            log.info("getActiveCount:" + threadPool.getActiveCount());
            log.info("getQueueSize:" + threadPool.getQueueSize());
            log.info("getCoreSize:" + threadPool.getCoreSize());
            log.info("getMaxSize:" + threadPool.getMaxSize());
            log.info("======================================");
            count++;
            TimeUnit.SECONDS.sleep(5);
            // 1min
            if (count >= 12) {
                break;
            }
        }

        threadPool.shutdown();

        Thread.currentThread().join();
    }
}
