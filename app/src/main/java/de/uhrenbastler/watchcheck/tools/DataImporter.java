package de.uhrenbastler.watchcheck.tools;


import android.support.v4.app.FragmentActivity;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.uhrenbastler.watchcheck.WatchCheckApplication;
import watchcheck.db.Log;
import watchcheck.db.LogDao;
import watchcheck.db.Watch;
import watchcheck.db.WatchDao;

/**
 * Created by clorenz on 24.01.15.
 */
public class DataImporter {

    FragmentActivity act;
    List<String> watchesData=new ArrayList<String>();
    List<String> logsData=new ArrayList<String>();
    LogDao logDao;
    WatchDao watchDao;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public void doImport(FragmentActivity act, String filename) throws Exception {
        logDao = ((WatchCheckApplication)act.getApplicationContext()).getDaoSession().getLogDao();
        watchDao = ((WatchCheckApplication)act.getApplicationContext()).getDaoSession().getWatchDao();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename)
                )
        );

        boolean readLogs = false;
        boolean isVersion2 = false;

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("WatchCheck2".equals(line)) {
                    isVersion2 = true;
                } else if (line.startsWith("----------------")) {
                    readLogs = true;          // only version 1
                } else {
                    parseLine(line, isVersion2, readLogs);
                }
            }
        } catch (Exception e) {
            Logger.error("Could not import data: " + e.getMessage(), e);
            return;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Logger.error("Could not close input: ", e);
                }
            }
        }

        if (isVersion2) {
            importDataVersion2();
        } else {
            importDataVersion1();
        }
    }


    private void parseLine(String rawData, boolean isVersion2, boolean isVersion1Logs) {
        if ( !isVersion2) {
            // Version 1:
            if ( isVersion1Logs) {
                logsData.add(rawData);
            } else {
                watchesData.add(rawData);
            }
        } else {
            if ( rawData.startsWith("WATCH: ")) {
                watchesData.add(rawData.substring(7));
            }
            if ( rawData.startsWith("LOG: ")) {
                logsData.add(rawData.substring(5));
            }
        }
    }


    private void importDataVersion1() throws Exception {
        watchDao.deleteAll();
        logDao.deleteAll();

        for ( String watchData : watchesData ) {
            if ( watchData.endsWith("|")) {
                watchData += "null";
            }
            String[] watchDataParts = watchData.split("\\|");
            Logger.debug("WatchData="+ArrayUtils.toString(watchDataParts));
            long watchId = Long.parseLong(watchDataParts[0]);
            String watchName = watchDataParts[1];
            String serial = watchDataParts[2];
            if ( "null".equals(serial)) {
                serial=null;
            }
            Date createDate = null;         // always null - bug in old app!
            String comment = watchDataParts[4];
            if ( "null".equals(comment)) {
                comment=null;
            }
            Watch watch = new Watch(watchId, watchName, serial, createDate, comment);
            watchDao.insert(watch);
        }

        int period=-1;
        int oldWatchId=-1;
        for ( String logData : logsData ) {
            if ( logData.endsWith("|")) {
                logData += "null";
            }
            String[] logDataParts = logData.split("\\|");
            Logger.debug("LogData="+ArrayUtils.toString(logDataParts));
            long logId = Long.parseLong(logDataParts[0]);
            int watchId = Integer.parseInt(logDataParts[1]);
            if ( watchId!=oldWatchId) {
                oldWatchId=watchId;
                period=-1;
            }
            boolean isNtp = "1".equals(logDataParts[2]);
            Date handyTime = sdf.parse(logDataParts[3]);
            Date referenceTime = new Date(handyTime.getTime() - (int)(1000d * Double.parseDouble(logDataParts[4])));
            long differenceInMillis = (long) (1000d * Double.parseDouble(logDataParts[5]));     // Precise difference in milliseconds

            long watchTimeInMillis = referenceTime.getTime() + differenceInMillis;

            // We assume, that the seconds of the timed watch are always exact zero!
            long watchTimeInMillisPrecisionSeconds = (long)Math.ceil((double)watchTimeInMillis / 1000) * 1000;

            long millisForWatchToZero =  watchTimeInMillisPrecisionSeconds - watchTimeInMillis;

            // This number of millisForWatchToZero has now to be added to the referenceTime
            referenceTime = new Timestamp(referenceTime.getTime() + millisForWatchToZero);

            Timestamp watchTime = new Timestamp(watchTimeInMillisPrecisionSeconds);
            if ( logDataParts[6].equals("1")) {
                // Reset
                period++;
            }
            String position = logDataParts[7];
            if ( "null".equals(position)) {
                position=null;
            }
            int temperature = Integer.parseInt(logDataParts[8]);
            String comment = logDataParts[9];
            if ( "null".equals(comment)) {
                comment=null;
            }

            Log log = new Log(logId, watchId, period, referenceTime, watchTime, position, temperature, comment);
            logDao.insert(log);
        }
    }


    private void importDataVersion2() throws Exception {
        watchDao.deleteAll();
        logDao.deleteAll();

        for (String watchData : watchesData) {
            String[] watchDataParts = watchData.split("\\|");
            long watchId = Long.parseLong(watchDataParts[0]);
            String watchName = watchDataParts[1];
            String serial = watchDataParts[2];
            Date createdAt=null;
            if ( !StringUtils.isBlank(watchDataParts[3])) {
                createdAt = new Date(Long.parseLong(watchDataParts[3]));
            }
            String comment = watchDataParts[4];
            Watch watch = new Watch(watchId, watchName, serial, createdAt, comment);
            watchDao.insert(watch);
        }

        for (String logData : logsData) {
            String[] logDataParts = logData.split("\\|");
            Long logId = Long.parseLong(logDataParts[0]);
            int watchId = Integer.parseInt(logDataParts[1]);
            int period = Integer.parseInt(logDataParts[2]);
            Date referenceTime = new Date(Long.parseLong(logDataParts[3]));
            Date watchTime = new Date(Long.parseLong(logDataParts[4]));
            String position = logDataParts[5];
            int temperature = Integer.parseInt(logDataParts[6]);
            String comment = logDataParts[7];
            Log log = new Log(logId, watchId, period, referenceTime, watchTime, position, temperature, comment);
            logDao.insert(log);
        }
    }
}
