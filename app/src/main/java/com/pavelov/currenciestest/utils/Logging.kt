package com.pavelov.currenciestest.utils

import android.util.Log

fun Any.logd(message: String) = Log.d(TAG(), message)
fun Any.logw(message: String) = Log.w(TAG(), message)
fun Any.loge(message: String) = Log.e(TAG(), message)
fun Any.loge(message: String, ex: Exception) = Log.e(TAG(), "", ex)

private fun Any.TAG() = "RatesApp/${this::class.java.simpleName}"