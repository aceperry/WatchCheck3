package de.uhrenbastler.watchcheck.provider;

import java.text.SimpleDateFormat;

/**
 * Created by clorenz on 09.01.15.
 */
public interface ITimeProvider {
    void setDateFormat(SimpleDateFormat sdf);

    String getTime();
    boolean isValid();
    boolean isGps();
    boolean isNtp();
    long getMillis();
    void terminate();
}
