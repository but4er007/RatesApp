package com.pavelov.currenciestest

import com.pavelov.currenciestest.network.CurrenciesApi
import com.pavelov.currenciestest.network.HttpClient
import com.pavelov.currenciestest.utils.AppExecutors
import com.pavelov.currenciestest.utils.ImageLoader

interface AppComponent {
    val httpClient: HttpClient
    val api: CurrenciesApi
    val ratesRepository: RatesRepository
    val executors: AppExecutors
    val imageLoader: ImageLoader
}

private class AppComponentImpl private constructor() : AppComponent {
    companion object {
        private val INSTANCE = AppComponentImpl()
        fun get() = INSTANCE
    }

    override val executors by lazy { AppExecutors() }
    override val httpClient by lazy { HttpClient() }
    override val api by lazy { CurrenciesApi(httpClient) }
    override val ratesRepository by lazy { RatesRepository(api) }
    override val imageLoader by lazy { ImageLoader() }
}

object ComponentProvider {
    fun app(): AppComponent = AppComponentImpl.get()
}

class RatesActivityComponent(private val activity: RatesActivity) {
    val appComponent = ComponentProvider.app()

    val ratesPullingInteractor by lazy {
        RatesPullingInteractor(
            appComponent.ratesRepository,
            appComponent.executors
        )
    }
    val ratesViewModel by lazy {
        activity.lastNonConfigurationInstance as? RatesViewModel
            ?: RatesViewModel(
                appComponent.executors,
                appComponent.ratesRepository,
                ratesPullingInteractor
            )
    }
    val ratesAdapter by lazy {
        RatesAdapter(
            activity,
            appComponent.executors,
            appComponent.imageLoader,
            ratesViewModel::updateBase,
            ratesViewModel::updateBaseAmount
        )
    }
}