package com.damn.anotherglass.logging;

import com.applicaster.xray.core.Logger;

public class ALog {

    private final Logger log;

    public ALog(Logger log) {
        this.log = log;
    }

    public void v(String tag, String message) {
        log.v(tag).message(message);
    }

    public void v(String tag, String message, Throwable throwable) {
        log.v(tag).exception(throwable).message(message);
    }

    public void d(String tag, String message) {
        log.d(tag).message(message);
    }

    public void d(String tag, String message, Throwable throwable) {
        log.d(tag).exception(throwable).message(message);
    }

    public void i(String tag, String message) {
        log.i(tag).message(message);
    }

    public void i(String tag, String message, Throwable throwable) {
        log.i(tag).exception(throwable).message(message);
    }

    public void w(String tag, String message) {
        log.w(tag).message(message);
    }

    public void w(String tag, String message, Throwable throwable) {
        log.w(tag).exception(throwable).message(message);
    }

    public void e(String tag, String message) {
        log.e(tag).message(message);
    }

    public void e(String tag, String message, Throwable throwable) {
        log.e(tag).exception(throwable).message(message);
    }
}
