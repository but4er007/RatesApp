package com.pavelov.currenciestest.utils

import android.content.Context
import android.os.SystemClock
import android.text.method.KeyListener
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.EditText

class DisablableEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditText(context, attrs, defStyleAttr) {
    private var editable = true
    private var lastMovementMethod: MovementMethod? = null
    private var lastKeyListener: KeyListener? = null

    fun setEditable(editable: Boolean) {
        if (this.editable == editable) return
        if (editable) {
            if (lastMovementMethod != null) {
                movementMethod = lastMovementMethod
            }
            if (lastKeyListener != null) {
                keyListener = lastKeyListener
            }
            isFocusable = true
            isFocusableInTouchMode = true
        } else {
            if (movementMethod != null) {
                lastMovementMethod = movementMethod
            }
            if (keyListener != null) {
                lastKeyListener = keyListener
            }
            movementMethod = null
            keyListener = null
            isFocusable = false
            isFocusableInTouchMode = false
        }
        this.editable = editable
    }

    fun dispatchClickEvent() {
        post {
            dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN,
                    width.toFloat(),
                    0f,
                    0
                )
            )
            dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    width.toFloat(),
                    0f,
                    0
                )
            )
        }
    }
}