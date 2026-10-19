package com.damn.anotherglass.logging;

import com.damn.xray.core.Logger;

public class ALog {

    private final Logger log;

    public ALog(Logger log) {
        this.log = log;
    }

    public void v(String tag, String message) {
        log.v(tag, message);
    }

    public void v(String tag, String message, Throwable throwable) {
        log.v(tag, message, throwable);
    }

    public void d(String tag, String message) {
        log.d(tag, message);
    }

    public void d(String tag, String message, Throwable throwable) {
        log.d(tag, message, throwable);
    }

    public void i(String tag, String message) {
        log.i(tag, message);
    }

    public void i(String tag, String message, Throwable throwable) {
        log.i(tag, message, throwable);
    }

    public void w(String tag, String message) {
        log.w(tag, message);
    }

    public void w(String tag, String message, Throwable throwable) {
        log.w(tag, message, throwable);
    }

    public void e(String tag, String message) {
        log.e(tag, message);
    }

    public void e(String tag, String message, Throwable throwable) {
        log.e(tag, message, throwable);
    }
}
