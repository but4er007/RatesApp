package com.pavelov.currenciestest.utils

import java.lang.ref.WeakReference
import java.util.*

class ValueObservable<T>() {
    private val observers = LinkedList<WeakReference<Observer<T>>>()
    var value: T? = null
        set(value) {
            val changed = value != field
            field = value
            if (changed) {
                observers.forEach {
                    it.get()?.onChanged(value)
                }
            }
        }

    /**
     * Subscribe to changes.
     * Immediately send current value to callback if not null.
     */
    fun observe(o: Observer<T>) {
        observers.add(WeakReference(o))
        value?.let(o::onChanged)
    }

    fun removeObserver(o: Observer<T>) {
        val i = observers.iterator()
        while (i.hasNext()) {
            val nextObserver = i.next().get()
            if (nextObserver == o || nextObserver == null) {
                i.remove()
            }
        }
    }

    interface Observer<T> {
        fun onChanged(value: T?)
    }
}