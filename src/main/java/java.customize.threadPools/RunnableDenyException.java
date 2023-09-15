package java.customize.threadPools;

/**
 * @
 * @date 2020/6/11
 */
public class RunnableDenyException extends RuntimeException {

    public RunnableDenyException(String message)
    {
        super(message);
    }
}
