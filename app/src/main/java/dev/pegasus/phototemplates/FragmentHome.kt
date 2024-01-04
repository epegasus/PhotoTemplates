package dev.pegasus.phototemplates

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dev.pegasus.phototemplates.databinding.FragmentHomeBinding
import dev.pegasus.phototemplates.helpers.managers.TextManager
import dev.pegasus.regret.RegretManager
import dev.pegasus.regret.enums.CaseType
import dev.pegasus.regret.interfaces.RegretListener
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.TextSticker
import dev.pegasus.stickers.helper.Sticker
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.HelperUtils.isValidPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentHome : Fragment() {

    private val globalContext by lazy { binding!!.root.context }

    private var binding: FragmentHomeBinding? = null
    private val dpTemplates by lazy { DpTemplates() }

    private val _regretManagerList = ArrayList<RegretManager>()
    private val regretManagerList: List<RegretManager> get() = _regretManagerList
    private var regretPosition = 0

    private val addTextString by lazy { resources.getString(R.string.add_text) }

    // need a global indicator for sticker update
    private var isStickerUpdating: Boolean = false
    private var newlyAddedTextSticker: TextSticker? = null
    // private var textBeforeUpdatingSticker: String? = null
    private val textManager by lazy { TextManager(globalContext) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initStickerView()

        binding?.apply {
            stvHome.onStickerOperationListener = stickerListener
            etTypeTemplate.addTextChangedListener(textWatcher)
            mbAddText.setOnClickListener { onAddTextClick() }
            ifvCloseTemplate.setOnClickListener { onCancelClick() }
            ifvDoneTemplate.setOnClickListener { onDoneClick() }
        }

    }

    private fun initUI() {
        binding?.tvHome?.setBackgroundFromModel(dpTemplates.list[0])
        binding?.tvHome?.setImageResource(R.drawable.img_pic)
    }

    private fun initStickerView() {
        binding?.stvHome?.isConstrained = true
        binding?.stvHome?.setLifecycleOwner(viewLifecycleOwner)
    }

    private fun onAddTextClick() {
        updateUI(true)
        binding?.etTypeTemplate?.let { showKeyboard(it) }
        addSticker()
    }

    private fun showKeyboard(editText: EditText) {
        forceShowKeyboard()
        editText.requestFocus()
    }

    private fun addSticker(newText: String = addTextString, isDuplicate: Boolean = false) {
        binding?.apply {
            etTypeTemplate.setText("")

            if (isDuplicate) {
                val txt = regretManagerList[regretPosition].getPreviousText()
                etTypeTemplate.setText(txt)
                etTypeTemplate.setSelection(txt.length)
            }

            val currentSticker = binding?.stvHome?.currentSticker
            if (currentSticker is TextSticker) {
                if (currentSticker.text == addTextString) return
            }
            lifecycleScope.launch {
                newlyAddedTextSticker = TextSticker(globalContext)
                newlyAddedTextSticker!!.text = newText
                newlyAddedTextSticker!!.setTextColor(Color.WHITE)
                newlyAddedTextSticker!!.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                newlyAddedTextSticker!!.resizeText()

                // Here, we don't update the sticker bcz it is a newly added sticker
                isStickerUpdating = false

                when (binding?.stvHome?.stickerCount!! < 20) {
                    true -> binding?.stvHome?.addSticker(newlyAddedTextSticker!!)
                    false -> root.let { Snackbar.make(it, resources.getString(R.string.limit_reached), Snackbar.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun onCancelClick() {
        updateUI(false)
        hideKeyboard()

        val currentSticker = binding?.stvHome?.currentSticker

        if (currentSticker is TextSticker) {
            if (isStickerUpdating) {
                currentSticker.text = regretManagerList[regretPosition].getPreviousText()
                currentSticker.resizeText()
                binding?.stvHome?.invalidate()
            } else {
                if (currentSticker.stickerId == newlyAddedTextSticker?.stickerId) {
                    binding?.stvHome?.removeCurrentSticker()
                } else {
                    currentSticker.text = regretManagerList[regretPosition].getPreviousText()
                    currentSticker.resizeText()
                    binding?.stvHome?.invalidate()
                }
            }
        }
    }

    private fun onDoneClick() {
        updateUI(false)
        hideKeyboard()
        val currentSticker = binding?.stvHome?.currentSticker
        if (currentSticker is TextSticker) {
            val text = currentSticker.text
            if (text.isNullOrEmpty() || text.isBlank() || text == addTextString) {
                binding?.stvHome?.removeCurrentSticker()
            }
            lifecycleScope.launch {
                textManager.applyNewText(currentSticker.text!!, binding?.stvHome!!, regretManagerList[regretPosition])
                //regretManagerList[regretPosition].setView(currentSticker)
            }
        }
    }

    private fun updateUI(showingKeyboard: Boolean) {
        binding?.mbAddText?.isVisible = !showingKeyboard
        binding?.ifvDoneTemplate?.isVisible = showingKeyboard
        binding?.ifvCloseTemplate?.isVisible = showingKeyboard
    }

    private val stickerListener = object : StickerView.OnStickerOperationListener {

        override fun onStickerAdded(sticker: Sticker) {
            if (sticker is TextSticker) addItemRegretManager(sticker)
        }

        override fun onStickerClicked(sticker: Sticker) {
            Log.d(TAG, "onStickerClicked: is called")
            lifecycleScope.launch {
                regretManagerList.forEachIndexed let@{ index, regretManager ->
                    if (regretManager.getView() == sticker) {
                        regretPosition = index
                        return@let
                    }
                }
                val txt = regretManagerList[regretPosition].getPreviousText()
                binding?.etTypeTemplate?.setText(txt)
                binding?.etTypeTemplate?.setSelection(txt.length)
            }
        }

        override fun onStickerDeleted(sticker: Sticker) {
            hideKeyboard()
            updateUI(false)
            if (regretManagerList.isNotEmpty() && regretManagerList.size > regretPosition) {
                _regretManagerList.removeAt(regretPosition)
            }
        }

        override fun onStickerTouchedDown(sticker: Sticker, isUpdate: Boolean, isDuplicate: Boolean) {
            Log.d(TAG, "onStickerTouchedDown: is called")
            isStickerUpdating = isUpdate
            if (sticker is TextSticker) {
                Log.d(TAG, "onStickerTouchedDown: sticker text: ${sticker.text}")
                if (isUpdate) {
                    updateUI(true)
                    binding?.etTypeTemplate?.let { showKeyboard(it) }
                }
                if (isDuplicate) {
                    addSticker(sticker.text.toString(), true)
                }
            }
        }

        override fun onStickerFlipped(sticker: Sticker) {}
        override fun onStickerZoomFinished(sticker: Sticker) {}
        override fun onStickerDragFinished(sticker: Sticker) {}
        override fun onStickerDoubleTapped(sticker: Sticker) {}
        override fun onStickerUpdated(sticker: Sticker) {}
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(text: Editable?) {
            Log.d(TAG, "afterTextChanged: $text")
            binding?.stvHome?.currentSticker?.let {
                if (it is TextSticker){
                    it.text = text.toString()
                    it.resizeText()
                    binding?.stvHome?.invalidate()
                }
            }
        }
    }

    private fun addItemRegretManager(sticker: TextSticker) {
        if (!isAdded) return
        val regretManager = RegretManager(globalContext, object : RegretListener {
            override fun onDo(key: CaseType, value: Any?, regretType: Int) {}
            override fun onCanDo(canUndo: Boolean, canRedo: Boolean) {}
        })
        _regretManagerList.add(regretManager)
        regretPosition = regretManagerList.size - 1
        if (regretPosition.isValidPosition(regretManagerList)) {
            regretManagerList[regretPosition].setView(sticker)
            textManager.applyDefaultText(regretManagerList[regretPosition], sticker.text!!)
            //textManager.applyDefaultTypeFace(regretManagerList[regretPosition], dpCollageText.getFontsList(0)[0].fontType)
            //textManager.applyDefaultColor(regretManagerList[regretPosition])
        }
    }

    private fun forceShowKeyboard() {
        val imm: InputMethodManager? = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun forceHideKeyboard() {
        val inputMethodManager: InputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view: View? = requireActivity().currentFocus
        if (view == null) {
            view = View(activity)
        }
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun hideKeyboard() {
        try {
            val inputMethodManager: InputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val view: IBinder? = activity?.findViewById<View?>(android.R.id.content)?.windowToken
            inputMethodManager.hideSoftInputFromWindow(view, 0)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}