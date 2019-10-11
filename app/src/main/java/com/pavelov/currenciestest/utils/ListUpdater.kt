package com.pavelov.currenciestest.utils

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pavelov.currenciestest.utils.ListUpdater.Callback
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Calculate diffs in background thread, save data consistency.
 *
 * [T] immutable data type. Adapter must save single object of this class, that represent
 * current state of list. [Callback.getCurrentHolder] must return this object,
 * [Callback.saveHolder] must replace this object.
 *
 * [callback] actual data callback
 */
class ListUpdater<T>(
    private val mainExecutor: Executor,
    private val calculationExecutor: Executor,
    private val adapter: RecyclerView.Adapter<*>,
    private val callback: Callback<T>
) {
    private val updatesQueue = LinkedList<(T) -> T>()
    private val updatingInProgress = AtomicBoolean(false)

    fun scheduleUpdate(holderProvider: (T) -> T) {
        synchronized(updatesQueue) {
            updatesQueue.add(holderProvider)
        }
        runUpdates()
    }

    private fun runUpdates() {
        synchronized(updatesQueue) {
            if (updatingInProgress.getAndSet(true)) return
            if (updatesQueue.isEmpty()) {
                updatingInProgress.set(false)
                return
            }
        }
        calculationExecutor.execute {
            val currentHolder = callback.getCurrentHolder()
            var updatedDataHolder = currentHolder
            val workingQueue: LinkedList<(T) -> T>
            synchronized(updatesQueue) {
                workingQueue = updatesQueue.clone() as LinkedList<(T) -> T>
                updatesQueue.clear()
            }
            workingQueue.forEach {
                updatedDataHolder = it.invoke(updatedDataHolder)
            }
            val diffResult = DiffUtil.calculateDiff(
                callback.createDiffCallback(
                    currentHolder,
                    updatedDataHolder
                )
            )
            mainExecutor.execute {
                callback.saveHolder(updatedDataHolder)
                diffResult.dispatchUpdatesTo(adapter)
                updatingInProgress.set(false)
                runUpdates()
            }
        }
    }

    interface Callback<T> {
        /** Return current data state */
        fun getCurrentHolder(): T

        /** Return diff callback for [T] */
        fun createDiffCallback(oldHolder: T, newHolder: T): DiffUtil.Callback

        /** Sahe new data state */
        fun saveHolder(newHolder: T)
    }
}