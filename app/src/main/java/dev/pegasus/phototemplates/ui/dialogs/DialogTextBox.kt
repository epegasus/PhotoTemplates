package dev.pegasus.phototemplates.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import dev.pegasus.phototemplates.commons.listeners.OnTextDoneClickListener
import dev.pegasus.phototemplates.databinding.DialogTextBoxBinding

class DialogTextBox : DialogFragment() {

    private val binding by lazy { DialogTextBoxBinding.inflate(layoutInflater) }
    private var onTextDoneClickListener: OnTextDoneClickListener? = null
    private var text: String? = null

    companion object {
        fun newInstance(text: String?): DialogTextBox {
            val dialogTextBox = DialogTextBox()
            val args = Bundle()
            args.putString("text", text)
            dialogTextBox.arguments = args
            return dialogTextBox
        }
    }

    fun setListener(onTextDoneClickListener: OnTextDoneClickListener) {
        this.onTextDoneClickListener = onTextDoneClickListener
    }

    private fun initializations() {
        text = arguments?.getString("text")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (binding.root.parent != null) {
            (binding.root.parent as ViewGroup).removeView(binding.root)
        }

        initializations()

        binding.ifvDoneTextBox.setOnClickListener { onDoneClick() }
        binding.ifvCloseTextBox.setOnClickListener { onCloseClick() }
        binding.etTextTextBox.addTextChangedListener(textWatcher())
        binding.etTextTextBox.setText(text)
        binding.etTextTextBox.requestFocus()

        // creating the fullscreen dialog
        val dialog = Dialog(binding.root.context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }

    private fun textWatcher() = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            p0?.let {
                if (it.trim().isEmpty()) binding.ifvDoneTextBox.visibility = View.GONE
                else binding.ifvDoneTextBox.visibility = View.VISIBLE
            } ?: kotlin.run { binding.ifvDoneTextBox.visibility = View.GONE }
        }

        override fun afterTextChanged(p0: Editable?) {}
    }

    private fun onDoneClick() {
        if (binding.etTextTextBox.text.toString().isNotEmpty()) {
            hideKeyboard()
            val isUpdate = text?.let { true } ?: false
            onTextDoneClickListener?.onDoneText(binding.etTextTextBox.text.toString(), isUpdate)
            dismiss()
        }
    }

    private fun onCloseClick() {
        binding.etTextTextBox.clearFocus()
        hideKeyboard()
        onTextDoneClickListener?.onCancelText()
        dismiss()
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager? = binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding.etTextTextBox.windowToken, 0)
    }

}