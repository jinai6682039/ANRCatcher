package com.github.alexhanxs.anrcatcher.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.alexhanxs.anrcatcher.ANRCatcher;
import com.github.alexhanxs.anrcatcher.ANRErrorInfo;
import com.github.alexhanxs.anrcatcher.R;

public class MainActivity extends AppCompatActivity {

    private final Object _mutex = new Object();

    private static void SleepAMinute() {
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ANRCatcher anrCatcher = ((ANRApplication) getApplication()).anrCatcher;

        findViewById(R.id.tv_sleep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SleepAMinute();
            }
        });

        findViewById(R.id.tv_sleep_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anrCatcher.setAnrCatchListener(new ANRCatcher.ANRCatchListener() {
                    @Override
                    public void onCatchANR(ANRErrorInfo e) {
                        Log.e(MainActivity.class.getSimpleName(), e.getMessage());
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        // 其他上报处理
                    }
                });
                SleepAMinute();
            }
        });

    }
}
