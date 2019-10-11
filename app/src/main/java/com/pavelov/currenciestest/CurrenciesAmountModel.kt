package com.pavelov.currenciestest

import androidx.annotation.WorkerThread
import com.pavelov.currenciestest.utils.*
import java.util.*
import kotlin.collections.ArrayList

class CurrenciesAmountModel(private val executors: AppExecutors) {
    private lateinit var rates: List<CurrencyRate>
    private lateinit var baseCode: String
    /**
     * This value hold actual currencies amount and order for view
     */
    val amountsObservable = ValueObservable<ArrayList<CurrencyAmountVO>>()


    fun updateBase(newBase: String) {
        logd("updateBase, newBase = $newBase")
        executors.calculationHandler.post {
            updateBaseInternal(newBase)
        }
    }

    fun updateBaseAmount(newBaseAmount: Double) {
        logd("updateBaseAmount, newBaseAmount = $newBaseAmount")
        executors.calculationHandler.post {
            recalculateAmounts(newBaseAmount)
        }
    }

    fun updateRates(serverRates: Rates) {
        logd("updateRates")
        executors.calculationHandler.post {
            updateRatesInternal(serverRates)
        }
    }

    @WorkerThread
    private fun updateRatesInternal(serverRates: Rates) {
        if (!::baseCode.isInitialized) {
            baseCode = serverRates.baseCurrencyCode
        }
        val newRates = mutableListOf<CurrencyRate>()
        if (baseCode == serverRates.baseCurrencyCode) {
            serverRates.currencyRates.forEach {
                newRates.add(CurrencyRate(it.currencyCode, it.rate))
            }
        } else {
            val currentBaseRateOnServerBase = serverRates.currencyRates
                .firstOrNull { it.currencyCode == baseCode }?.rate
            if (currentBaseRateOnServerBase == null) {
                // валюта, которая сейчас является основанием, не пришла с сервера
                // нужно заменить текущее основание тем, что пришло с сервера и запустить эту
                // функцию заново
                logw("updateRates -> base [$baseCode] absent in new server rates")
                baseCode = serverRates.baseCurrencyCode
                updateRates(serverRates)
                return
            }
            val serverBaseNewRate = 1.0 / currentBaseRateOnServerBase
            newRates.add(CurrencyRate(serverRates.baseCurrencyCode, serverBaseNewRate))
            serverRates.currencyRates.forEach {
                if (it.currencyCode != baseCode) {
                    newRates.add(CurrencyRate(it.currencyCode, it.rate * serverBaseNewRate))
                }
            }
        }
        rates = newRates
        recalculateAmounts()
    }

    @WorkerThread
    private fun updateBaseInternal(newBase: String) {
        if (!::rates.isInitialized) {
            baseCode = newBase
            return
        }

        if (newBase == baseCode) {
            // nothing to update
            return
        }

        val newBaseOldRate = rates.firstOrNull { it.currencyCode == newBase }?.rate

        if (newBaseOldRate == null) {
            loge("updateBase -> new base [$newBase] absent in current rates")
            return
        }

        val oldBaseNewRate = 1.0 / newBaseOldRate
        val newRates = mutableListOf<CurrencyRate>()
        newRates.add(CurrencyRate(baseCode, oldBaseNewRate))
        rates.forEach {
            if (it.currencyCode != newBase) {
                newRates.add(CurrencyRate(it.currencyCode, it.rate * oldBaseNewRate))
            }
        }

        baseCode = newBase
        rates = newRates
        recalculateAmounts()
    }

    @WorkerThread
    private fun recalculateAmounts(newBaseAmount: Double? = null) {
        logd("recalculateAmounts, newBaseAmount = $newBaseAmount")
        if (!::rates.isInitialized || !::baseCode.isInitialized) {
            logw("recalculateAmounts -> rates not initialized")
            return
        }
        val amounts = amountsObservable.value

        val baseAmount = newBaseAmount
            ?: amounts?.firstOrNull { it.currencyCode == baseCode }?.currencyAmount
            ?: 1.0

        // new amounts list
        val newAmounts = ArrayList<CurrencyAmountVO>(rates.size)
        // what currencies already added in list
        val processedCurrencies = LinkedList<String>()

        processedCurrencies.add(baseCode)
        newAmounts.add(CurrencyAmountVO(baseCode, baseAmount))

        amounts?.forEach { amount ->
            if (amount.currencyCode != baseCode) {
                rates.firstOrNull { it.currencyCode == amount.currencyCode }?.let { rate ->
                    processedCurrencies.add(rate.currencyCode)
                    newAmounts.add(CurrencyAmountVO(rate.currencyCode, baseAmount * rate.rate))
                }
            }
        }

        // all new rates add to bottom of list
        rates.forEach { rate ->
            if (!processedCurrencies.contains(rate.currencyCode)) {
                newAmounts.add(CurrencyAmountVO(rate.currencyCode, baseAmount * rate.rate))
            }
        }

        executors.mainHandler.post {
            amountsObservable.value = newAmounts
        }
    }

}