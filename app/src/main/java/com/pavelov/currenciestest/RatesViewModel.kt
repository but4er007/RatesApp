package com.pavelov.currenciestest

import com.pavelov.currenciestest.utils.AppExecutors
import com.pavelov.currenciestest.utils.LoadingStatus
import com.pavelov.currenciestest.utils.ValueObservable

class RatesViewModel(
    executors: AppExecutors,
    private val repository: RatesRepository,
    private val interactor: RatesPullingInteractor
) {
    private val currencyAmountModel = CurrenciesAmountModel(executors)
    val currencyNames = ValueObservable<List<CurrencyName>>()
    val initStatus = ValueObservable<LoadingStatus>()
    val currenciesAmount = currencyAmountModel.amountsObservable

    private val fetchCurrencyNamesStatusObserver =
        object : ValueObservable.Observer<LoadingStatus> {
            override fun onChanged(value: LoadingStatus?) {
                mergeLoadingStatuses()
            }
        }

    private val initFetchRatesStatusObserver = object : ValueObservable.Observer<LoadingStatus> {
        override fun onChanged(value: LoadingStatus?) {
            mergeLoadingStatuses()
        }
    }

    private val ratesObserver = object : ValueObservable.Observer<Rates> {
        override fun onChanged(value: Rates?) {
            if (value == null) return
            currencyAmountModel.updateRates(value)
        }
    }

    private val currencyNamesObserver = object : ValueObservable.Observer<List<CurrencyName>> {
        override fun onChanged(value: List<CurrencyName>?) {
            value ?: return
            currencyNames.value = value
        }
    }

    init {
        repository.rates.observe(ratesObserver)
        repository.currencyNames.observe(currencyNamesObserver)
        repository.currencyNamesFetchingStatus.observe(fetchCurrencyNamesStatusObserver)
        interactor.initFetchStatus.observe(initFetchRatesStatusObserver)
        repository.fetchCurrencyNames()
    }

    fun start() {
        interactor.startPulling()
    }

    fun stop() {
        interactor.stopPulling()
    }

    fun updateBaseAmount(baseAmount: Double) {
        currencyAmountModel.updateBaseAmount(baseAmount)
    }

    fun updateBase(base: String) {
        currencyAmountModel.updateBase(base)
    }

    fun retryInit() {
        repository.fetchCurrencyNames()
        interactor.resetInitPullingError()
    }

    private fun mergeLoadingStatuses() {
        if (interactor.initFetchStatus.value == LoadingStatus.LOADING
            || repository.currencyNamesFetchingStatus.value == LoadingStatus.LOADING
        ) {
            initStatus.value = LoadingStatus.LOADING
            return
        }
        if (interactor.initFetchStatus.value == LoadingStatus.ERROR
            || repository.currencyNamesFetchingStatus.value == LoadingStatus.ERROR
        ) {
            initStatus.value = LoadingStatus.ERROR
            return
        }
        initStatus.value = LoadingStatus.SUCCESS
    }
}
