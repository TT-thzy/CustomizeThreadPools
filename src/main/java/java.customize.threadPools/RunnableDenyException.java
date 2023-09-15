package java.customize.threadPools;

/**
 * @author TT-berg
 * @date 2022/3/18
 */
public class RunnableDenyException extends RuntimeException {

    public RunnableDenyException(String message)
    {
        super(message);
    }
}
