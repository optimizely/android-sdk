package com.optimizely.ab.android.sdk;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class OptimizelyLiteLogger implements Logger {

    private final String tag;

    public OptimizelyLiteLogger(String name) {
        tag = name;
    }

    @Override
    public String getName() {

        return "OptimizelyLiteLogger";
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {

    }

    @Override
    public void trace(String format, Object arg) {

    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {

    }

    @Override
    public void trace(String format, Object... arguments) {

    }

    @Override
    public void trace(String msg, Throwable t) {

    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {

    }

    @Override
    public void trace(Marker marker, String format, Object arg) {

    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {

    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {

    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {

    }

    @Override
    public boolean isDebugEnabled() {
        if (BuildConfig.DEBUG) {
            return true;
        }

        return false;
    }

    @Override
    public void debug(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg );
        }

    }

    @Override
    public void debug(String format, Object arg) {
        Log.d(tag, formatMessage(format, arg) );
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        Log.d(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        Log.d(tag, formatMessage(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        Log.d(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        Log.d(tag, formatMessage(format, arg));
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        Log.d(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        Log.d(tag, formatMessage(format, arguments));
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        Log.d(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isInfoEnabled() {
        if (BuildConfig.DEBUG) {
            return true;
        }
        return false;
    }

    @Override
    public void info(String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void info(String format, Object arg) {
        Log.i(tag, formatMessage(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        Log.i(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        Log.i(tag, formatMessage(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        Log.i(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        Log.i(tag, formatMessage(format, arg));
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        Log.i(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        Log.i(tag, formatMessage(format, arguments));
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        Log.i(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        Log.w(tag,  msg);

    }

    @Override
    public void warn(String format, Object arg) {
        Log.w(tag, formatMessage(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        Log.w(tag, formatMessage(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        Log.w(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        Log.w(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        Log.w(tag, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        Log.w(tag, formatMessage(format, arg));
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        Log.w(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        Log.w(tag, formatMessage(format, arguments));
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        Log.w(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        Log.e(tag, msg);

    }

    @Override
    public void error(String format, Object arg) {
        Log.e(tag, formatMessage(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        Log.e(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        Log.e(tag, formatMessage(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        Log.e(tag, formatThrowable(msg, t));
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        Log.e(tag, formatMessage(format, arg));
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        Log.e(tag, formatMessage(format, arg1, arg2));
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        Log.e(tag, formatMessage(format, arguments));
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        Log.e(tag, formatThrowable(msg, t));
    }

    private String formatMessage(String format, Object... arguments) {
        for (Object arg : arguments) {
            format = format.replaceFirst("\\{\\}", arg.toString());
        }

        return format;
    }

    private String formatThrowable(String msg, Throwable t) {
        return new StringBuilder(msg).append("\n").append(t.getMessage()).toString();
    }
}
