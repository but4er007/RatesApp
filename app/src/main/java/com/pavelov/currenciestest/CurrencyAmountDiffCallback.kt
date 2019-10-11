package com.pavelov.currenciestest

import androidx.recyclerview.widget.DiffUtil

class CurrencyAmountDiffCallback(
    private val oldItems: List<CurrencyAmountVO>,
    private val newItems: List<CurrencyAmountVO>,
    private val firstItemInEditMode: Boolean
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldItems.size
    override fun getNewListSize() = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldItems[oldItemPosition].currencyCode == newItems[newItemPosition].currencyCode

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        (oldItemPosition == 0 && newItemPosition == 0 && firstItemInEditMode)
                || oldItems[oldItemPosition] == newItems[newItemPosition]
                && ((oldItemPosition != 0) == (newItemPosition != 0))
}