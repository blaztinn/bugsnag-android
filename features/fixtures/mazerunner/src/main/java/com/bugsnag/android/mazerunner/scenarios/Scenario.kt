package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Context

import com.bugsnag.android.*

abstract internal class Scenario(
    protected val config: Configuration,
    protected val context: Context
): Application.ActivityLifecycleCallbacks {

    var eventMetaData: String? = null

    open fun run() {

    }

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    protected fun disableSessionDelivery() {
        val baseDelivery = Bugsnag.getClient().config.delivery
        Bugsnag.getClient().config.delivery = object: Delivery {
            override fun deliver(payload: SessionTrackingPayload, config: Configuration) {
                throw DeliveryFailureException("Session Delivery NOP", RuntimeException("NOP"))
            }

            override fun deliver(report: Report, config: Configuration) {
                baseDelivery.deliver(report, config)
            }
        }
    }

    /**
     * Sets a NOP implementation for the Error Tracking API, preventing delivery
     */
    protected fun disableReportDelivery() {
        val baseDelivery = Bugsnag.getClient().config.delivery
        Bugsnag.getClient().config.delivery = object: Delivery {
            override fun deliver(payload: SessionTrackingPayload, config: Configuration) {
                baseDelivery.deliver(payload, config)
            }

            override fun deliver(report: Report, config: Configuration) {
                throw DeliveryFailureException("Session Delivery NOP", RuntimeException("NOP"))
            }
        }

    }

    /**
     * Sets a NOP implementation for the Error Tracking API and the Session Tracking API,
     * preventing delivery
     */
    @Deprecated("Disable via config instead, using the new delivery API")
    protected fun disableAllDelivery() {
        disableSessionDelivery()
        disableReportDelivery()
    }

    protected fun disableAllDelivery(config: Configuration) {
        config.delivery = object: Delivery {
            override fun deliver(payload: SessionTrackingPayload, config: Configuration) {
                throw DeliveryFailureException("Error Delivery NOP", RuntimeException("NOP"))
            }

            override fun deliver(report: Report, config: Configuration) {
                throw DeliveryFailureException("Session Delivery NOP", RuntimeException("NOP"))
            }
        }
    }

    /**
     * Returns a throwable with the message as the current classname
     */
    protected fun generateException(): Throwable = RuntimeException(javaClass.simpleName)


    /* Activity lifecycle callback overrides */

    protected fun registerActivityLifecycleCallbacks() {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}

}
