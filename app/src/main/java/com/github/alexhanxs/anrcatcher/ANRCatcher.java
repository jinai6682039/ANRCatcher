package com.github.alexhanxs.anrcatcher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Alexhanxs on 2018/6/28.
 */

public class ANRCatcher extends Thread {

    public interface WorkSleepInterruptedListener {
        void onCatchInterrupted(InterruptedException e);
    }

    public interface ANRCatchListener {
        void onCatchANR(ANRErrorInfo e);
    }

    private static int DEFAULT_WORK_SLEEP_TIME = 5 * 1000;

    private static final ANRCatchListener DEFAULT_ANR_LISTENER = new ANRCatchListener() {
        @Override
        public void onCatchANR(ANRErrorInfo e) {
            throw e;
        }
    };

    private static final WorkSleepInterruptedListener DEFAULT_INTERRUPTION_LISTENER = new WorkSleepInterruptedListener() {
        @Override
        public void onCatchInterrupted(InterruptedException exception) {
            Log.w("ANRCatcher", "Interrupted: " + exception.getMessage());
        }
    };

    private static int work = 0;
    private static Handler uiHandler = new Handler(Looper.getMainLooper());

    private static Runnable detectWork = new Runnable() {
        @Override
        public void run() {
            work = (work + 1) % 1000;
        }
    };

    private ANRCatchListener anrCatchListener = DEFAULT_ANR_LISTENER;
    private WorkSleepInterruptedListener workSleepInterruptedListener = DEFAULT_INTERRUPTION_LISTENER;

    private int sleepTime = DEFAULT_WORK_SLEEP_TIME;

    public ANRCatcher() {

    }

    public ANRCatcher(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {

        int lastWork;
        while (!isInterrupted()) {
            lastWork = work;
            uiHandler.post(detectWork);

            try {
                Thread.sleep(DEFAULT_WORK_SLEEP_TIME);
            } catch (InterruptedException e) {
                if (workSleepInterruptedListener != null) {
                    workSleepInterruptedListener.onCatchInterrupted(e);
                }
            }

            /**
             * 当主线程阻塞超过 {@link sleepTime )所指示的时间后，我们就认为已经发生了ANR
             */
            if (lastWork == work) {

                ANRErrorInfo error = ANRErrorInfo.createANR();
                if (anrCatchListener != null) {
                    anrCatchListener.onCatchANR(error);
                }
                return;
            }
        }
    }

    public ANRCatchListener getAnrCatchListener() {
        return anrCatchListener;
    }

    public void setAnrCatchListener(ANRCatchListener anrCatchListener) {
        this.anrCatchListener = anrCatchListener;
    }

    public WorkSleepInterruptedListener getWorkSleepInterruptedListener() {
        return workSleepInterruptedListener;
    }

    public void setWorkSleepInterruptedListener(WorkSleepInterruptedListener workSleepInterruptedListener) {
        this.workSleepInterruptedListener = workSleepInterruptedListener;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }
}
