package org.example.app.ui.settings

import android.text.Editable
import android.text.TextWatcher

/**
 * Lightweight TextWatcher wrapper so screens can only implement the callback they need.
 */
class SimpleTextWatcher(private val onChange: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun afterTextChanged(s: Editable?) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onChange(s?.toString().orEmpty())
    }
}
