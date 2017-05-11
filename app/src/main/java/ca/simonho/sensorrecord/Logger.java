package ca.simonho.sensorrecord;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A logger that uses the standard Android Log class to log exceptions, and also logs them to a
 * file on the device. Requires permission WRITE_EXTERNAL_STORAGE in AndroidManifest.xml.
 * @author Cindy Potvin
 */
public class Logger{

    private File logFileDir;
    private File logFile;

    //Constructor
    public Logger(File logFileDir){
        this.logFileDir = logFileDir;
        this.logFile = new File(logFileDir, "log.txt");
    }

    /**
     * Sends an error message to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public void e(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.ERROR))
            return;

        int logResult = Log.e(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    /**
     * Sends an error message and the exception to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public void e(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.ERROR))
            return;

        int logResult = Log.e(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    /**
     * Sends a warning message to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public void w(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.WARN))
            return;

        int logResult = Log.w(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    /**
     * Sends a warning message and the exception to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public void w(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.WARN))
            return;

        int logResult = Log.w(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    /**
     * Sends an info message to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public void i(Context context, String logMessageTag, String logMessage){
        if (!Log.isLoggable(logMessageTag, Log.INFO))
            return;

        int logResult = Log.i(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    /**
     * Sends an info message and the exception to LogCat and to a log file.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public void i(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        if (!Log.isLoggable(logMessageTag, Log.INFO))
            return;

        int logResult = Log.i(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    /**
     * Sends a verbose message to LogCat.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public void v(Context context, String logMessageTag, String logMessage){
        // If the build is not debug, do not try to log, the logcat be
        // stripped at compilation.
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.VERBOSE))
            return;

        int logResult = Log.v(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    /**
     * Sends a verbose message and the exception to LogCat.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public void v(Context context, String logMessageTag, String logMessage, Throwable throwableException){
        // If the build is not debug, do not try to log, the logcat be
        // stripped at compilation.
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.VERBOSE))
            return;

        int logResult = Log.v(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    /**
     * Sends a debug message to LogCat.
     * @param context The context of the application.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     */
    public void d(Context context, String logMessageTag, String logMessage){
        // If the build is not debug, do not try to log, the logcat be
        // stripped at compilation.
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.DEBUG))
            return;

        int logResult = Log.d(logMessageTag, logMessage);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage);
    }

    /**
     * Sends a debug message and the exception to LogCat.
     * @param logMessageTag A tag identifying a group of log messages. Should be a constant in the
     *                      class calling the logger.
     * @param logMessage The message to add to the log.
     * @param throwableException An exception to log
     */
    public void d(Context context,String logMessageTag, String logMessage, Throwable throwableException){
        // If the build is not debug, do not try to log, the logcat be
        // stripped at compilation.
        if (!BuildConfig.DEBUG || !Log.isLoggable(logMessageTag, Log.DEBUG))
            return;

        int logResult = Log.d(logMessageTag, logMessage, throwableException);
        if (logResult > 0)
            logToFile(context, logMessageTag, logMessage + "\r\n" + Log.getStackTraceString(throwableException));
    }

    /**
     * Gets a stamp containing the current date and time to write to the log.
     * @return The stamp for the current date and time.
     */
    private String getDateTimeStamp(){
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.CANADA).format(dateNow));
    }

    /**
     * Writes a message to the log file on the device.
     * @param logMessageTag A tag identifying a group of log messages.
     * @param logMessage The message to add to the log.
     */
    private void logToFile(Context context, String logMessageTag, String logMessage){
        try{
            //Create log directory if it doesn't already exist
            if (!logFileDir.exists()) {
                Boolean created = logFileDir.mkdirs();
            }

            // Gets the log file from primary storage. If it does
            // not exist, the file is created.
            if (!logFile.exists())
                logFile.createNewFile();

            // Write the message to the log with a timestamp
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s [%2s]: %3s\r\n", getDateTimeStamp(), logMessageTag, logMessage));
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug to see the latest
            // changes
            MediaScannerConnection.scanFile(context,
                    new String[]{logFile.toString()},
                    null,
                    null);
        }
        catch (IOException e){
            Log.e("Logger", "Unable to log exception to file.");
        }
    }
}