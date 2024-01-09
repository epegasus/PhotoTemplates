package dev.pegasus.template

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

class TemplateEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): AppCompatEditText(context, attrs, defStyleAttr) {

    // This code is for handling keyboard ime action
    /*init {
        setOnKeyListener { _, keyCode, event ->
            when (event?.action) {
                KeyEvent.ACTION_DOWN -> Log.d(TAG, "onKeyDown: keyCode = $keyCode")
                KeyEvent.ACTION_UP -> Log.d(TAG, "onKeyUp: keyCode = $keyCode")
            }
            false
        }
    }*/

    interface OnKeyboardSystemBackButtonClick {
        fun onBackButtonPressed()
    }

    private var onBackButtonPressedListener: OnKeyboardSystemBackButtonClick? = null

    fun setOnBackButtonPressedListener(listener: OnKeyboardSystemBackButtonClick) {
        onBackButtonPressedListener = listener
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            onBackButtonPressedListener?.onBackButtonPressed()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    fun showKeyboard() {
        val imm: InputMethodManager? = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun hideKeyboard() {
        val imm: InputMethodManager? = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

}