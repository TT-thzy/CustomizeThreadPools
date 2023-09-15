package java.customize.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

/**
 * @
 * @date 2020/3/18
 */
public final class ThreadUtil {

    public static void sleepForMilliSeconds(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleepForSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleepForMilliSecondsWithThrowable(int milliseconds) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(milliseconds);
    }

    public static String exceptionToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        throwable.printStackTrace(writer);
        return stringWriter.toString();
    }

}
