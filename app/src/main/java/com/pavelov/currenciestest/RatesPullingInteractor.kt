package com.pavelov.currenciestest

import com.pavelov.currenciestest.utils.AppExecutors
import com.pavelov.currenciestest.utils.LoadingStatus
import com.pavelov.currenciestest.utils.ValueObservable

class RatesPullingInteractor(
    private val repository: RatesRepository,
    private val executors: AppExecutors
) {
    companion object {
        private const val RATES_UPDATE_DELAY = 1000L
        private const val FAILING_RATES_FETCHING_BEFORE_ERROR = 6
    }

    private val launchFetchHandler = executors.calculationHandler
    /** SUCCESS if at least one fetching successful */
    val initFetchStatus = ValueObservable<LoadingStatus>()
    /** true, if last [FAILING_RATES_FETCHING_BEFORE_ERROR] fetches failed in row. */
    val fetchingError = ValueObservable<Boolean>()
    private var failedFetchingCount = 0

    init {
        initFetchStatus.value = LoadingStatus.LOADING
        fetchingError.value = false
    }

    fun resetInitPullingError() {
        if (initFetchStatus.value != LoadingStatus.SUCCESS) {
            initFetchStatus.value = LoadingStatus.LOADING
        }
    }

    fun startPulling() {
        repository.ratesFetchingStatus.observe(fetchStatusObserver)
        launchFetchHandler.post(startFetchRunnable)
    }

    fun stopPulling() {
        repository.ratesFetchingStatus.removeObserver(fetchStatusObserver)
        launchFetchHandler.removeCallbacks(startFetchRunnable)
    }

    private val fetchStatusObserver = object : ValueObservable.Observer<LoadingStatus> {
        override fun onChanged(value: LoadingStatus?) {
            when (value) {
                LoadingStatus.ERROR -> {
                    failedFetchingCount++
                    if (failedFetchingCount > FAILING_RATES_FETCHING_BEFORE_ERROR) {
                        fetchingError.value = true
                        if (initFetchStatus.value == LoadingStatus.LOADING) {
                            initFetchStatus.value = LoadingStatus.ERROR
                        }
                    }
                    launchFetchHandler.postDelayed(startFetchRunnable, RATES_UPDATE_DELAY)
                }
                LoadingStatus.SUCCESS -> {
                    failedFetchingCount = 0
                    if (fetchingError.value == true) {
                        fetchingError.value = false
                    }
                    if (initFetchStatus.value == LoadingStatus.LOADING || initFetchStatus.value == LoadingStatus.ERROR) {
                        initFetchStatus.value = LoadingStatus.SUCCESS
                    }
                    launchFetchHandler.postDelayed(startFetchRunnable, RATES_UPDATE_DELAY)
                }
                else -> {
                }
            }
        }
    }

    private val startFetchRunnable = Runnable {
        repository.fetchRates(null)
    }
}