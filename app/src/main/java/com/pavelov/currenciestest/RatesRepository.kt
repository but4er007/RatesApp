package com.pavelov.currenciestest

import android.os.AsyncTask
import com.pavelov.currenciestest.network.CurrenciesApi
import com.pavelov.currenciestest.network.Response
import com.pavelov.currenciestest.utils.LoadingStatus
import com.pavelov.currenciestest.utils.ValueObservable
import com.pavelov.currenciestest.utils.logd

class RatesRepository(private val api: CurrenciesApi) {
    private var ratesFetchingTask: AsyncTask<Void, Void, Response<Rates>>? = null
    private var currencyNamesFetchingTask: AsyncTask<Void, Void, Response<List<CurrencyName>>>? =
        null
    val rates = ValueObservable<Rates>()
    val ratesFetchingStatus = ValueObservable<LoadingStatus>()
    val currencyNames = ValueObservable<List<CurrencyName>>()
    val currencyNamesFetchingStatus = ValueObservable<LoadingStatus>()

    fun fetchRates(base: String?) {
        logd("fetchRates")
        if (
            ratesFetchingTask != null
            && ratesFetchingTask?.status != AsyncTask.Status.FINISHED
        ) {
            return
        }
        ratesFetchingStatus.value = LoadingStatus.LOADING
        ratesFetchingTask = LoadRatesTask(base, api, object : AsyncTaskCallback<Rates> {
            override fun onSuccess(value: Rates) {
                rates.value = value
                ratesFetchingStatus.value = LoadingStatus.SUCCESS
            }

            override fun onError() {
                ratesFetchingStatus.value = LoadingStatus.ERROR
            }
        }).execute()
    }

    fun fetchCurrencyNames() {
        logd("fetchCurrencyNames")
        if (
            currencyNamesFetchingTask != null
            && currencyNamesFetchingTask?.status != AsyncTask.Status.FINISHED
        ) {
            return
        }
        currencyNamesFetchingTask =
            LoadCurrencyNamesTask(api, object : AsyncTaskCallback<List<CurrencyName>> {
                override fun onSuccess(value: List<CurrencyName>) {
                    currencyNames.value = value
                    currencyNamesFetchingStatus.value = LoadingStatus.SUCCESS
                }

                override fun onError() {
                    currencyNamesFetchingStatus.value = LoadingStatus.ERROR
                }
            }).execute()
    }

    private class LoadCurrencyNamesTask(
        private val api: CurrenciesApi,
        private val callback: AsyncTaskCallback<List<CurrencyName>>
    ) : AsyncTask<Void, Void, Response<List<CurrencyName>>>() {

        override fun doInBackground(vararg params: Void?) = api.getCurrencyNames()

        override fun onPostExecute(result: Response<List<CurrencyName>>?) {
            if (result == null || result.hasError || result.data == null) {
                callback.onError()
                return
            }
            callback.onSuccess(result.data)
        }
    }

    private class LoadRatesTask(
        private val base: String?,
        private val api: CurrenciesApi,
        private val callback: AsyncTaskCallback<Rates>
    ) : AsyncTask<Void, Void, Response<Rates>>() {

        override fun doInBackground(vararg params: Void?) = api.getCurrencies(base)

        override fun onPostExecute(result: Response<Rates>?) {
            if (result == null || result.hasError || result.data == null) {
                callback.onError()
                return
            }
            callback.onSuccess(result.data)
        }
    }

    private interface AsyncTaskCallback<T> {
        fun onSuccess(value: T)
        fun onError()
    }
}


