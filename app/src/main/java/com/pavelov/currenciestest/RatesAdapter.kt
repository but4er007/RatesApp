package com.pavelov.currenciestest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.pavelov.currenciestest.utils.AppExecutors
import com.pavelov.currenciestest.utils.ImageLoader
import com.pavelov.currenciestest.utils.ListUpdater
import kotlinx.android.synthetic.main.item_rate.view.*
import java.util.*


class RatesAdapter(
    context: Context,
    executors: AppExecutors,
    private val imageLoader: ImageLoader,
    private val rateClickListener: (String) -> Unit,
    private val onBaseAmountChanged: (Double) -> Unit
) : RecyclerView.Adapter<RatesAdapter.ViewHolder>() {
    private val inflater = LayoutInflater.from(context)
    private val listUpdater = ListUpdater(
        executors.mainExecutor,
        executors.calculationExecutor,
        this,
        UpdaterCallback()
    )
    private val displayWidth: Int = context.let {
        val wm = (it.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return@let 0
        val size = Point()
        wm.defaultDisplay.getSize(size)
        return@let size.x
    }
    /** Size of flag in pixel */
    private val flagSize = (context.resources.displayMetrics.density * FLAG_SIZE_DP).toInt()
    private var amounts: List<CurrencyAmountVO> = emptyList()
    /** If true, first currency just changed, need to request focus and open keyboard */
    private var firstItemJustChanged = true
    /** First item is in edit mode. No need for update him amount */
    private var firstItemInEditMode = false
    /** New first item, need to scroll top */
    var onFirstItemChanged: (() -> Unit)? = null
    var currencyNames: List<CurrencyName>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateItems(_currencyAmounts: List<CurrencyAmountVO>) {
        listUpdater.scheduleUpdate { _currencyAmounts }
    }

    override fun getItemCount() = amounts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.item_rate, parent, false)
        view.rate_value.maxWidth = displayWidth / 2
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position, amounts[position])
    }

    inner class UpdaterCallback : ListUpdater.Callback<List<CurrencyAmountVO>> {
        override fun getCurrentHolder() = amounts
        override fun createDiffCallback(
            oldHolder: List<CurrencyAmountVO>,
            newHolder: List<CurrencyAmountVO>
        ) = CurrencyAmountDiffCallback(oldHolder, newHolder, firstItemInEditMode)

        override fun saveHolder(newHolder: List<CurrencyAmountVO>) {
            if (amounts.firstOrNull()?.currencyCode != newHolder.firstOrNull()?.currencyCode) {
                onFirstItemChanged?.invoke()
                firstItemJustChanged = true
            }
            amounts = newHolder
        }
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val rateEditText = itemView.rate_value

        private val baseAmountChangeListener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (adapterPosition != 0) return
                s ?: return
                val input = s.toString()
                if (input.isEmpty()) {
                    onBaseAmountChanged.invoke(0.0)
                    return
                }
                val value = input.toDoubleOrNull() ?: return
                onBaseAmountChanged.invoke(value)
            }
        }

        private val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (adapterPosition != 0) return@OnFocusChangeListener
            if (!hasFocus) {
                val imm =
                    v.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                firstItemInEditMode = false
            } else {
                firstItemInEditMode = true
            }
        }

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position == 0) {
                    rateEditText.dispatchClickEvent()
                } else {
                    rateClickListener.invoke(amounts[position].currencyCode)
                }
            }
            rateEditText.onFocusChangeListener = focusChangeListener
            rateEditText.addTextChangedListener(baseAmountChangeListener)
        }

        fun bind(position: Int, rate: CurrencyAmountVO) {
            if (itemView.rate_id.text.toString() != rate.currencyCode) {
                itemView.rate_id.text = rate.currencyCode
                itemView.rate_description.text = currencyNames?.getDisplayNameFor(rate.currencyCode)
                imageLoader.loadCircleFlagInto(itemView.flag_image, rate.currencyCode, flagSize)
            }
            if (position == 0 && !rateEditText.hasFocus()) {
                rateEditText.setText("%.2f".format(Locale.US, rate.currencyAmount))
                rateEditText.setEditable(true)
                if (firstItemJustChanged) {
                    rateEditText.dispatchClickEvent()
                    firstItemJustChanged = false
                }
            } else {
                rateEditText.setText("%.2f".format(Locale.US, rate.currencyAmount))
                rateEditText.setEditable(false)
            }
        }

    }

    companion object {
        private const val FLAG_SIZE_DP = 32
    }
}