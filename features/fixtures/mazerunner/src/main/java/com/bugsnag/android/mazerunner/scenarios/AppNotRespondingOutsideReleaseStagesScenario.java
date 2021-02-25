package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

public class AppNotRespondingOutsideReleaseStagesScenario extends Scenario {

    public AppNotRespondingOutsideReleaseStagesScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.setNotifyReleaseStages(new String[]{"fee-fi-fo-fum"});
        Handler main = new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50000); // Forever
                } catch (Exception _ex) {
                    // Catch possible thread interruption exception
                }
            }
        }, 1); // Delayed to allow the UI to appear so there is something to tap
    }
}
