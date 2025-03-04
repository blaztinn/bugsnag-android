package com.bugsnag.android

import java.io.IOException

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
internal class ThreadState @JvmOverloads constructor(
    exc: Throwable?,
    isUnhandled: Boolean,
    sendThreads: ThreadSendPolicy,
    projectPackages: Collection<String>,
    logger: Logger,
    currentThread: java.lang.Thread = java.lang.Thread.currentThread(),
    stackTraces: MutableMap<java.lang.Thread, Array<StackTraceElement>> = java.lang.Thread.getAllStackTraces()
) : JsonStream.Streamable {

    internal constructor(
        exc: Throwable?,
        isUnhandled: Boolean,
        config: ImmutableConfig
    ) : this(exc, isUnhandled, config.sendThreads, config.projectPackages, config.logger)

    val threads: MutableList<Thread>

    init {
        val recordThreads = sendThreads == ThreadSendPolicy.ALWAYS ||
            (sendThreads == ThreadSendPolicy.UNHANDLED_ONLY && isUnhandled)

        threads = when {
            recordThreads -> captureThreadTrace(
                stackTraces,
                currentThread,
                exc,
                isUnhandled,
                projectPackages,
                logger
            )
            else -> mutableListOf()
        }
    }

    private fun captureThreadTrace(
        stackTraces: MutableMap<java.lang.Thread, Array<StackTraceElement>>,
        currentThread: java.lang.Thread,
        exc: Throwable?,
        isUnhandled: Boolean,
        projectPackages: Collection<String>,
        logger: Logger
    ): MutableList<Thread> {
        // API 24/25 don't record the currentThread, add it in manually
        // https://issuetracker.google.com/issues/64122757
        if (!stackTraces.containsKey(currentThread)) {
            stackTraces[currentThread] = currentThread.stackTrace
        }
        if (exc != null && isUnhandled) { // unhandled errors use the exception trace for thread traces
            stackTraces[currentThread] = exc.stackTrace
        }

        val currentThreadId = currentThread.id
        return stackTraces.keys
            .sortedBy { it.id }
            .map {
                val stacktrace = Stacktrace.stacktraceFromJavaTrace(stackTraces[it]!!, projectPackages, logger)
                val errorThread = it.id == currentThreadId
                Thread(it.id, it.name, ThreadType.ANDROID, errorThread, stacktrace, logger)
            }.toMutableList()
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginArray()
        for (thread in threads) {
            writer.value(thread)
        }
        writer.endArray()
    }
}
