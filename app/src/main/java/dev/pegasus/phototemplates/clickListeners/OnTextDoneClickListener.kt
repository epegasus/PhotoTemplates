package dev.pegasus.phototemplates.clickListeners

interface OnTextDoneClickListener {
    fun onDoneText(text: String, isUpdate: Boolean)
    fun onCancelText()
}