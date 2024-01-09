package dev.pegasus.phototemplates

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import dev.pegasus.template.TemplateEditText
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.HelperUtils.isValidPosition
import kotlinx.coroutines.launch

class FragmentHome : Fragment(), TemplateEditText.OnKeyboardSystemBackButtonClick {

    private val globalContext by lazy { binding!!.root.context }

    private var binding: FragmentHomeBinding? = null
    private val dpTemplates by lazy { DpTemplates() }

    private val _regretManagerList = ArrayList<RegretManager>()
    private val regretManagerList: List<RegretManager> get() = _regretManagerList
    private var regretPosition = 0

    private val addTextString by lazy { resources.getString(R.string.add_text) }

    // need a global indicator for sticker update
    private var isStickerUpdating: Boolean = false
    private var isKeyboardOpened: Boolean = false
    private var newlyAddedTextSticker: TextSticker? = null
    private val textManager by lazy { TextManager(globalContext) }
    private var selectedStickerId: Int? = null

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
            etTypeTemplate.setOnBackButtonPressedListener(this@FragmentHome)
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
        showKeyboard()
        addSticker()
    }

    private fun showKeyboard() {
        isKeyboardOpened = true
        binding?.etTypeTemplate?.showKeyboard()
        binding?.etTypeTemplate?.requestFocus()
    }

    private fun addSticker(newText: String = addTextString, stickerToBeDuplicated: Sticker? = null) {
        binding?.apply {
            etTypeTemplate.setText("")

            stickerToBeDuplicated?.let {
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
                    true -> binding?.stvHome?.addSticker(newlyAddedTextSticker!!, stickerToBeDuplicated)
                    false -> root.let { Snackbar.make(it, resources.getString(R.string.limit_reached), Snackbar.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun onCancelClick() {
        updateUI(false)
        binding?.etTypeTemplate?.hideKeyboard()
        isKeyboardOpened = false

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
        binding?.etTypeTemplate?.hideKeyboard()
        isKeyboardOpened = false

        val currentSticker = binding?.stvHome?.currentSticker
        if (currentSticker is TextSticker) {
            val text = currentSticker.text
            if (text.isNullOrEmpty() || text.isBlank() || text == addTextString) {
                binding?.stvHome?.removeCurrentSticker()
            }
            lifecycleScope.launch {
                textManager.applyNewText(currentSticker.text!!, binding?.stvHome!!, regretManagerList[regretPosition])
            }
        }
    }

    private fun updateUI(showingKeyboard: Boolean) {
        binding?.mbAddText?.isVisible = !showingKeyboard
        binding?.ifvDoneTemplate?.isVisible = showingKeyboard
        binding?.ifvCloseTemplate?.isVisible = showingKeyboard
        binding?.backgroundView?.isVisible = showingKeyboard
    }

    private val stickerListener = object : StickerView.OnStickerOperationListener {

        override fun onStickerAdded(sticker: Sticker) {
            if (sticker is TextSticker) {
                addItemRegretManager(sticker)
                selectedStickerId = sticker.stickerId
            }
        }

        override fun onStickerClicked(sticker: Sticker) {
            Log.d(TAG, "onStickerClicked: is called")
            val currentSticker = binding?.stvHome?.currentSticker
            if (currentSticker is TextSticker) {
                if (currentSticker.stickerId == selectedStickerId) return
                selectedStickerId = currentSticker.stickerId
                lifecycleScope.launch {
                    regretManagerList.forEachIndexed let@{ index, regretManager ->
                        if (regretManager.getView() == sticker) {
                            regretPosition = index
                            return@let
                        }
                    }
                    val txt = regretManagerList[regretPosition].getPreviousText()
                    if (txt == addTextString) binding?.etTypeTemplate?.setText("")
                    else {
                        binding?.etTypeTemplate?.setText(txt)
                        binding?.etTypeTemplate?.setSelection(txt.length)
                    }
                }
            }
        }

        override fun onStickerDeleted(sticker: Sticker) {
            updateUI(false)
            binding?.etTypeTemplate?.hideKeyboard()
            isKeyboardOpened = false

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
                    showKeyboard()
                }
                if (isDuplicate) {
                    addSticker(sticker.text.toString(), sticker)
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

    override fun onBackButtonPressed() {
        onCancelClick()
    }

}