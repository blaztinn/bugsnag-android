package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.TestHarnessHooksKt;

import androidx.annotation.NonNull;

public class CXXStopSessionScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    private Handler handler = new Handler();

    public native int crash(int counter);

    public CXXStopSessionScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.getClient().startSession();
        Bugsnag.getClient().stopSession();
        TestHarnessHooksKt.flushAllSessions();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                crash(0);
            }
        }, 500);
    }
}
