package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 9/13/13
 * Time: 12:15 PM
 * To change this template use File | Settings | File Templates.
 */
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.google.analytics.tracking.android.ExceptionParser;

public class AnalyticsExceptionParser implements ExceptionParser {
    /*
    * (non-Javadoc)
    * @see com.google.analytics.tracking.android.ExceptionParser#getDescription(java.lang.String, java.lang.Throwable)
    */
    public String getDescription(String p_thread, Throwable p_throwable) {
        return "Thread: " + p_thread + ", Exception: " + ExceptionUtils.getStackTrace(p_throwable);
    }

}
