package com.github.alexhanxs.anrcatcher.sample;

import android.app.Application;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.alexhanxs.anrcatcher.ANRCatcher;
import com.github.alexhanxs.anrcatcher.ANRErrorInfo;

/**
 * Created by Alexhanxs on 2018/6/28.
 */

public class ANRApplication extends Application {

    public ANRCatcher anrCatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        anrCatcher = new ANRCatcher();
        anrCatcher.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {

                if (e instanceof ANRErrorInfo) {
                    try {
                        //延迟2秒杀进程
                        new Thread() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                //在此处处理出现异常的情况
                                Toast.makeText(ANRApplication.this,
                                        "由于抛出了未捕获的ANR异常，这里将在2s后退出应用", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }
                        }.start();
                        Thread.sleep(2000);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    } catch (InterruptedException ee) {
                        Log.e(ANRApplication.class.getSimpleName(), "error : ", ee);
                    }

                }
            }
        });
        anrCatcher.start();
    }
}
