package com.bugsnag.android

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks whether the app is currently in its launch period. This creates a timer of
 * configuration.launchDurationMillis, after which which the launch period is considered
 * complete. If this value is zero, then the user must manually call markLaunchCompleted().
 */
internal class LaunchCrashTracker(config: ImmutableConfig) : BaseObservable() {

    private val launching = AtomicBoolean(true)
    private val executor = ScheduledThreadPoolExecutor(1)
    private val logger = config.logger

    init {
        val delay = config.launchDurationMillis

        if (delay > 0) {
            executor.executeExistingDelayedTasksAfterShutdownPolicy = false
            try {
                executor.schedule({ markLaunchCompleted() }, delay, TimeUnit.MILLISECONDS)
            } catch (exc: RejectedExecutionException) {
                logger.w("Failed to schedule timer for LaunchCrashTracker", exc)
            }
        }
    }

    fun markLaunchCompleted() {
        executor.shutdown()
        launching.set(false)
        notifyObservers(StateEvent.UpdateIsLaunching(false))
        logger.d("App launch period marked as complete")
    }

    fun isLaunching() = launching.get()
}
