package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

public class CXXJavaUserInfoNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    public CXXJavaUserInfoNativeCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.setUser("9816734", "j@example.com", "Strulyegha  Ghaumon  Rabelban  Snefkal  Angengtai  Samperris  Dreperwar Raygariss  Haytther  Ackworkin  Turdrakin  Clardon");
        crash();
    }
}
