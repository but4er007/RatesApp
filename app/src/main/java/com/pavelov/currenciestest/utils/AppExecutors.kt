package com.pavelov.currenciestest.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.Executor

class AppExecutors {
    val mainHandler = Handler(Looper.getMainLooper())
    val mainExecutor = Executor { mainHandler.post(it) }
    val calculationHandler = HandlerThread("calculationHandlerThread").let {
        it.priority = 3
        it.start()
        Handler(it.looper)
    }
    val calculationExecutor = Executor { calculationHandler.post(it) }
}