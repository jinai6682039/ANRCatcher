# A way to Catch ANR Info

捕获ANR和捕获应用异常是不同的，应用异常crash是会通过主线程设置的UncaughtExceptionHandler来进行捕获处理。而ANR并没有一个完整的抛出点，所以我们需要另辟蹊径。

## MainThread、Looper、Handler、MessageQueue、ActivityThread
在进行分析之前如何进行ANR的捕获前，这里需要对MainThread、Looper、Handler、MessageQueue、ActivityThread这几个类之间的关系做一个介绍，这个将是我们进行ANR捕获的一个关键点。

### 一切的开始--创建ActivityThread
在AMS启动组件时，发现其将运行所在的进程不存在时，会调用AMS的方法来创建一个新的ActivityThread，作为此组件将要运行的进程。
在创建ActivityThread时，会调用ActivityThread.main()方法。在此方法中会去创建一个新的ActivityThread对象，然后和AMS相关联起来，并初始化Instrumentation、LoadedApk等对象，调用对应Appliction.onCreate()方法，最后会在AMS.attachApplicitionLocked()中来启动对应组件。

除此之外，在ActivityThread.main()方法中，会通过Looper.perpareMainLooper()来创建主线程，也就是UI线程。在创建完主线程之后，会继续调用Looper.loop()来为主线程来创建一个MessageQueue，用来接受Handler发生的message，然后开启MessageQueue的循环接受消息工作。

之后如果获取了通过 new Handler(Looper.getMainLooper())来获取一个主线程的Handler后，可以通过handler.post或postDelayed()来在主线程上执行一段代码。这两个方法都会将对要执行的Runnable封装为一个Message，然后发往主线程的MessageQueue中排队等待处理。

### ANR发生的几种情形

首先会在Service（无论是使用StartService还是bindService）进行启动（调用）的Service，其每一次AMS远程调用ActivityThread相应的生命周期函数的时间大于一定时间（前台Service20s、后台Service200s）时，都会触发AMS的AppErrors来提示ANR Dialog。

此外当串行发生广播时（对静态注册的BroadcastReceiver发生普通广播或有序广播、对动态注册的BroadcastReceiver发送有序广播），若单一一个BroadcastReceiver处理时间大于一定的时间（对于前台BroadcastQueue来处理的Broadcast是10s，而后台BroadcastQueue处理的Broadcast是60s）时，也会触发AMS的AppErrors来展示ANR Dialog。

同时在处理输入框响应等其他情况时，若处理超时一定的时间(5s)，也会触发AMS的AppErrors来展示相应的ANR Dialog。

上面无论是ActivityThread处理Service生命周期函数超时、BroadcastReceiver.onReceive()方法的处理超时（静态注册的BroadcastReceiver将在ActivityThread中进行处理，而动态注册的BroadcastReceiver若不指明处理的线程，则也会默认在主线程中处理）。这也就意味着此时的主线程已经阻塞了。

而对于其他情形，如在主线程处理耗时工作（如直接简单粗暴的sleep），也会将主线程给阻塞起来，最后触发AppErrors来展示ANR Dialog。

可以说触发ANR的主要原因就是主线程被阻塞了，导致下一个MessageQueue中的Message无法及时处理，触发AMS的AppErrors来展示ANR。
**总而言之就是主线程被阻塞，无法执行其他Message。**
**总而言之就是主线程被阻塞，无法执行其他Message。**
**总而言之就是主线程被阻塞，无法执行其他Message。**

重要的事情要说三遍！
那既然知道了这个重点，那么接下来的工作就好做了。

## 捕获ANR

对于之前所说的，那我们可以通过拿到一个主线程的对应Handler来post一个检测线程，在这个检测线程中会循环间隔一定时间来执行一个检测工作，当某次检测工作发现主线程已经阻塞，那么我们就有理由怀疑此时已经发生了ANR。

当然，这个检测线程需要尽可能早的post到主线程中去。所以，这个情况下，我们可以放到主线程的较早的一个入口，Application.onCreate()中去执行（Application.onCreate()会在AMS和新创建的ActivityThread进行绑定时进行调用。

## 关键代码
```java
    private static int work = 0;
    // 获取主线程handler
    private static Handler uiHandler = new Handler(Looper.getMainLooper());
	// 检测值修改
    private static Runnable detectWork = new Runnable() {
        @Override
        public void run() {
            work = (work + 1) % 1000;
        }
    };

	// 检测线程的实际工作
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
```

```java
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
                    try {//延迟2秒杀进程
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
```