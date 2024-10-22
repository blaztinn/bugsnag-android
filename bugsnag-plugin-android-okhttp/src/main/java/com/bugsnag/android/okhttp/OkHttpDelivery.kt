package com.bugsnag.android.okhttp

import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.Session
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OkHttpDelivery(
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
    private val logger: Logger? = null,
) : Delivery {
    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val requestBody = payload.toByteArray().toRequestBody()

        val call = client.newCall(
            Request.Builder()
                .url(deliveryParams.endpoint)
                .headers(deliveryParams.toHeaders())
                .post(requestBody)
                .build()
        )

        val response = call.execute()
        return DeliveryStatus.forHttpResponseCode(response.code)
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        try {
            val requestBody = payload.toByteArray().toRequestBody()
            val integrityHeader = payload.integrityToken

            val requestBuilder = Request.Builder()
                .url(deliveryParams.endpoint)

            if (integrityHeader != null) {
                requestBuilder.header("Bugsnag-Integrity", integrityHeader)
            }

            requestBuilder
                .headers(deliveryParams.toHeaders())
                .post(requestBody)

            val call = client.newCall(requestBuilder.build())
            val response = call.execute()

            return DeliveryStatus.forHttpResponseCode(response.code)
        } catch (oom: OutOfMemoryError) {
            // attempt to persist the payload on disk. This approach uses streams to write to a
            // file, which takes less memory than serializing the payload into a ByteArray, and
            // therefore has a reasonable chance of retaining the payload for future delivery.
            logger?.w("Encountered OOM delivering payload, falling back to persist on disk", oom)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: IOException) {
            logger?.w("IOException encountered in request", exception)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            logger?.w("Unexpected error delivering payload", exception)
            return DeliveryStatus.FAILURE
        }
    }

    private fun DeliveryParams.toHeaders(): Headers {
        return Headers.Builder().run {
            headers.forEach { (name, value) ->
                value?.let { add(name, it) }
            }

            build()
        }
    }
}
