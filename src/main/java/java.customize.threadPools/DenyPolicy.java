package java.customize.threadPools;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
public interface DenyPolicy {

    void reject(Runnable runnable, ThreadPool threadPool);

    /**
     * 直接丢弃
     */
    class DiscardDenyPolicy implements DenyPolicy {

        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            //do nothing
        }
    }

    /**
     * 向任务提交者抛出异常
     */
    class AbortDenyPolicy implements DenyPolicy {

        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            throw new RunnableDenyException("The runnable " + runnable + " will be abort.");
        }
    }

    /**
     * 该拒绝策略会使任务在提交者所在的线程中执行任务
     */
    class RunnerDenyPolicy implements DenyPolicy {

        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            if (!threadPool.isShutdown()) {
                runnable.run();
            }
        }
    }
}
