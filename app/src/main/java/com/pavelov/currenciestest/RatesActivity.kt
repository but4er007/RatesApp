package com.pavelov.currenciestest

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pavelov.currenciestest.utils.LoadingStatus
import com.pavelov.currenciestest.utils.ValueObservable
import kotlinx.android.synthetic.main.activity_main.*


class RatesActivity : Activity() {
    private lateinit var component: RatesActivityComponent
    private lateinit var viewModel: RatesViewModel
    private lateinit var adapter: RatesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = RatesActivityComponent(this)
        setContentView(R.layout.activity_main)
        viewModel = component.ratesViewModel
        adapter = component.ratesAdapter
        adapter.onFirstItemChanged = ::scrollToTop

        rates_list.apply {
            layoutManager = LinearLayoutManager(this@RatesActivity)
            adapter = this@RatesActivity.adapter
            itemAnimator = object : DefaultItemAnimator() {
                override fun canReuseUpdatedViewHolder(
                    vh: RecyclerView.ViewHolder,
                    p: MutableList<Any>
                ) = true
            }
        }
        rates_list.setHasFixedSize(true)
        retry_btn.setOnClickListener { viewModel.retryInit() }
    }

    /** save view model between configuration changes */
    override fun onRetainNonConfigurationInstance() = viewModel

    override fun onStart() {
        super.onStart()
        viewModel.start()
        viewModel.initStatus.observe(initStatusObserver)
        viewModel.currencyNames.observe(currencyNamesObserver)
        viewModel.currenciesAmount.observe(currenciesAmountObserver)
    }

    override fun onStop() {
        viewModel.initStatus.removeObserver(initStatusObserver)
        viewModel.currencyNames.removeObserver(currencyNamesObserver)
        viewModel.currenciesAmount.removeObserver(currenciesAmountObserver)
        viewModel.stop()
        super.onStop()
    }

    private fun scrollToTop() {
        rates_list.post {
            (rates_list.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(0, 0)
        }
    }

    private val initStatusObserver = object : ValueObservable.Observer<LoadingStatus> {
        override fun onChanged(value: LoadingStatus?) {
            value ?: return
            val (content, progress, error) = when (value) {
                LoadingStatus.LOADING -> Triple(View.INVISIBLE, View.VISIBLE, View.INVISIBLE)
                LoadingStatus.ERROR -> Triple(View.INVISIBLE, View.INVISIBLE, View.VISIBLE)
                LoadingStatus.SUCCESS -> Triple(View.VISIBLE, View.INVISIBLE, View.INVISIBLE)
            }
            rates_list.visibility = content
            progress_view.visibility = progress
            error_view.visibility = error
        }
    }

    private val currencyNamesObserver = object : ValueObservable.Observer<List<CurrencyName>> {
        override fun onChanged(value: List<CurrencyName>?) {
            value ?: return
            adapter.currencyNames = value
        }
    }

    private val currenciesAmountObserver =
        object : ValueObservable.Observer<ArrayList<CurrencyAmountVO>> {
            override fun onChanged(value: ArrayList<CurrencyAmountVO>?) {
                value ?: return
                adapter.updateItems(value)
            }
        }
}
