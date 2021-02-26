package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Bundle

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.lang.Thread
import android.os.Handler
import android.os.HandlerThread

/**
 * Sends a handled exception to Bugsnag, which has a short delay to allow the app to remain
 * in the foreground for ~1 second
 */
internal class InForegroundScenario(config: Configuration,
                                    context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        registerActivityLifecycleCallbacks()
    }

    override fun onActivityStopped(activity: Activity) {
        Bugsnag.notify(generateException())
    }

}
