package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.TestHarnessHooksKt;

import androidx.annotation.NonNull;

public class CXXSessionInfoCrashScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int crash(int value);

    private Handler handler = new Handler();

    public CXXSessionInfoCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.startSession();
        Bugsnag.notify(new Exception("For the first"));
        Bugsnag.notify(new Exception("For the second"));
        TestHarnessHooksKt.flushAllSessions();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                crash(3837);
            }
        }, 500);
    }
}
