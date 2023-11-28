package dev.pegasus.phototemplates.commons.listeners

interface OnTextDoneClickListener {
    fun onDoneText(text: String, isUpdate: Boolean)
    fun onCancelText()
}