package dev.pegasus.regret

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import dev.pegasus.regret.enums.CaseType
import dev.pegasus.regret.helper.Regret
import dev.pegasus.regret.interfaces.RegretListener
import dev.pegasus.stickers.TextSticker
import dev.pegasus.stickers.ui.DrawableSticker

class RegretManager(private val context: Context, private val regretListener: RegretListener) : RegretListener {

    private val regret = Regret(this)
    private var textSticker: TextSticker? = null
    private var drawableSticker: DrawableSticker? = null

    private var fontTypeArrayList = ArrayList<String>()
    private var originalFontTypeArrayList = ArrayList<String>()
    val fontTypeList: List<String> get() = fontTypeArrayList

    // Previous Settings
    private var previousText = ""
    private var previousTypeface: Typeface? = null
    private var previousTextColor = 0

    private var isUndoing = false
    var isBold = false
    var isItalic = false
    var isUnderline = false
    var isStrikeThrough = false

    fun setView(textSticker: TextSticker) {
        this.textSticker = textSticker
    }

    fun setView(drawableSticker: DrawableSticker) {
        this.drawableSticker = drawableSticker
    }

    fun getView(): TextSticker? {
        return textSticker
    }

    /* ------------------------------------- Text ------------------------------------- */

    fun setPreviousText(previousText: String) {
        this.previousText = previousText
    }

    fun setNewText(newText: String) {
        if (!isUndoing) {
            regret.add(CaseType.TEXT, previousText, newText)
            previousText = newText
        }
    }

    fun getPreviousText(): String = this.previousText

    /* ---------------------------------- TypeFace ---------------------------------- */

    fun setPreviousTypeFace(previousTypeface: Typeface, fontType: String) {
        this.previousTypeface = previousTypeface
        fontTypeArrayList.add(fontType)
        originalFontTypeArrayList.add(fontType)
    }

    fun setNewTypeFace(newTypeface: Typeface, fontType: String) {
        previousTypeface?.let {
            if (!isUndoing) {
                if (!canUndo())
                    originalFontTypeArrayList.clear()
                regret.add(CaseType.TYPEFACE, it, newTypeface)
                fontTypeArrayList.add(fontType)
                originalFontTypeArrayList.add(fontType)
                //previousTypeface = newTypeface
            }
        } ?: throw NullPointerException("Previous Typeface is required. Set it using 'setPreviousTypeFace()'")
    }

    /* -------------------------------- Text Color ---------------------------------- */

    fun setPreviousTextColor(previousTextColor: Int) {
        this.previousTextColor = previousTextColor
    }

    fun setNewTextColor(newTextColor: Int) {
        if (!isUndoing) {
            regret.add(CaseType.TEXT_COLOR, previousTextColor, newTextColor)
            //previousTextColor = newTextColor
        }
    }

    fun undo() {
        if (regret.canUndo()) {
            isUndoing = true
            regret.undo()
            isUndoing = false
        }
    }

    fun redo() {
        if (regret.canRedo()) {
            isUndoing = true
            regret.redo()
            isUndoing = false
        }
    }

    fun canUndo() = regret.canUndo()
    fun canRedo() = regret.canRedo()

    /**
     * @param key
     * @param value
     * @param regretType
     *      0: Undo
     *      1: Redo
     */

    override fun onDo(key: CaseType, value: Any?, regretType: Int) {
        when (key) {
            CaseType.TEXT -> textSticker?.text = value.toString()
            CaseType.TYPEFACE -> {
                if (regretType == 0)
                    fontTypeArrayList.removeLast()
                else {
                    fontTypeArrayList.add(originalFontTypeArrayList[fontTypeList.size - 1])
                }
                textSticker?.setTypeface(value as Typeface?)
            }
            CaseType.TEXT_COLOR -> textSticker?.setTextColor(ContextCompat.getColor(context, value as Int))
        }
    }

    override fun onCanDo(canUndo: Boolean, canRedo: Boolean) {
        regretListener.onCanDo(canUndo, canRedo)
    }
}