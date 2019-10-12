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

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val itemDontCareAboutChanges = oldItemPosition == 0 && newItemPosition == 0 && firstItemInEditMode
        if (itemDontCareAboutChanges) return true
        if (oldItems[oldItemPosition] != newItems[newItemPosition]) return false
        // if item change editable status - it need to bind
        val itemDidntChangedEditableStatus = (oldItemPosition != 0) == (newItemPosition != 0)
        return itemDidntChangedEditableStatus
    }
}