package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.storage.StorageManager

import com.bugsnag.android.*
import com.bugsnag.android.Configuration
import java.io.File

internal class PartialSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        config.beforeSend { true }

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("eventMetaData")
            val dir = File(context.cacheDir, "bugsnag-sessions")

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val files = dir.listFiles()
                files.forEach { it.writeText("{[]}") }
            }
        }
    }

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.startSession()
        }

        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post {
            flushAllSessions()
        }
    }
}
