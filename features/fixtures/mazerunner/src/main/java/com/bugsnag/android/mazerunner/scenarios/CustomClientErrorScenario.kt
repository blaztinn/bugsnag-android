package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends a handled exception to Bugsnag using a custom API client which modifies the request.
 */
internal class CustomClientErrorScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {

    init {
        config.delivery = createCustomHeaderDelivery(context)
    }

    override fun run() {

        super.run()
        Bugsnag.notify(RuntimeException("Hello"))
    }

}
