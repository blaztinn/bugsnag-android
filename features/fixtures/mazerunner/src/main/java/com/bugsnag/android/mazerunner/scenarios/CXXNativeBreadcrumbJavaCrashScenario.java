package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;

public class CXXNativeBreadcrumbJavaCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNativeBreadcrumbJavaCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        activate();
        String[] items = new String[]{"one","two"};
        System.out.println("Last item is: " + items[2]);
    }
}
