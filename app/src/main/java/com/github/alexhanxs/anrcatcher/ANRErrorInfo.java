package com.github.alexhanxs.anrcatcher;

import android.os.Looper;

import java.io.Serializable;

/**
 * Created by Alexhanxs on 2018/6/28.
 */

public class ANRErrorInfo extends Error {

    private ANRErrorInfo(ANRError.ThreadInfo info) {
        super("Application Not Responding", info);
    }

    private static class ANRError implements Serializable {
        private final String _name;
        private final StackTraceElement[] _stackTrace;

        private class ThreadInfo extends Throwable {
            private ThreadInfo(ThreadInfo other) {
                super(_name, other);
            }

            @Override
            public Throwable fillInStackTrace() {
                setStackTrace(_stackTrace);
                return this;
            }
        }

        private ANRError(String name, StackTraceElement[] stackTrace) {
            _name = name;
            _stackTrace = stackTrace;
        }
    }

    static ANRErrorInfo createANR() {
        final Thread mainThread = Looper.getMainLooper().getThread();
        final StackTraceElement[] mainStackTrace = mainThread.getStackTrace();

        return new ANRErrorInfo(new ANRError(getThreadTitle(mainThread), mainStackTrace)
                .new ThreadInfo(null));
    }

    private static String getThreadTitle(Thread thread) {
        return thread.getName() + " (state = " + thread.getState() + ")";
    }
}
