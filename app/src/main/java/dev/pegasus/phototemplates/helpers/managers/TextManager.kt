package dev.pegasus.phototemplates.helpers.managers

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import dev.pegasus.phototemplates.R
import dev.pegasus.regret.RegretManager
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.TextSticker

class TextManager(private val context: Context) {

    /* ------------------------------------------------- Font ------------------------------------------------- */

    private fun getHandlerThreadHandler(): Handler {
        val handlerThread = HandlerThread("fonts")
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    /*fun applyDefaultTypeFace(regretManager: RegretManager, fontType: String) {
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            fontType,
            R.array.com_google_android_gms_fonts_certs
        )

        val callback = object : FontsContractCompat.FontRequestCallback() {
            override fun onTypefaceRetrieved(typeface: Typeface?) {
                super.onTypefaceRetrieved(typeface)
                Log.d(TAG, "onTypefaceRetrieved: called")

                typeface?.let { tf ->
                    regretManager.setPreviousTypeFace(tf, fontType)
                } ?: kotlin.run {
                    //val runtimeException = RuntimeException("Cannot set Typeface after applying font in Regret Manager. (FontManager.kt > applyFont)")
                    //runtimeException.recordException("Text Manager")
                }
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                super.onTypefaceRequestFailed(reason)
                Log.d(TAG, "onTypefaceRequestFailed: called")
            }
        }
        FontsContractCompat.requestFont(context, request, callback, getHandlerThreadHandler())
    }

    fun applyFont(queryType: String, textView: TextView) {
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            queryType,
            R.array.com_google_android_gms_fonts_certs
        )

        val callback = object : FontsContractCompat.FontRequestCallback() {
            override fun onTypefaceRetrieved(typeface: Typeface?) {
                super.onTypefaceRetrieved(typeface)
                textView.typeface = typeface
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                super.onTypefaceRequestFailed(reason)
                Log.d(TAG, "onTypefaceRequestFailed: called")
            }
        }
        FontsContractCompat.requestFont(context, request, callback, getHandlerThreadHandler())
    }

    fun applyFont(fontType: String, stickerView: StickerView, regretManager: RegretManager) {
        stickerView.currentSticker?.let {
            val request = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                fontType,
                R.array.com_google_android_gms_fonts_certs
            )

            val requestCallback = object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface?) {
                    super.onTypefaceRetrieved(typeface)
                    Log.d(TAG, "onTypefaceRetrieved: called")
                    (it as TextSticker).setTypeface(typeface)
                    stickerView.invalidate()

                    typeface?.let { tf ->
                        try {
                            regretManager.setNewTypeFace(tf, fontType)
                        } catch (ex: NullPointerException) {
                            //ex.recordException("Text Manager")
                        }
                    } ?: kotlin.run {
                        //val runtimeException = RuntimeException("Cannot Update Typeface after applying font in Regret Manager. (FontManager.kt > applyFont)")
                        //runtimeException.recordException("Text Manager")
                    }
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    super.onTypefaceRequestFailed(reason)
                    Log.d(TAG, "onTypefaceRequestFailed: called")
                }
            }
            FontsContractCompat.requestFont(context, request, requestCallback, getHandlerThreadHandler())
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyFont", "elvis", "Current Sticker is null")
        }
    }*/

    /* ------------------------------------------------- Text ------------------------------------------------- */

    fun applyDefaultText(regretManager: RegretManager, text: String) {
        regretManager.setPreviousText(text)
    }

    fun applyNewText(newText: String, stickerView: StickerView, regretManager: RegretManager) {
        stickerView.currentSticker?.let {
            regretManager.setNewText(newText)
            (it as TextSticker).text = newText
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyNewText", "elvis", "Current Sticker is null")
        }
    }

    /* ------------------------------------------------- Color ------------------------------------------------- */

    fun applyDefaultColor(regretManager: RegretManager) {
        regretManager.setPreviousTextColor(R.color.white)
    }

    fun applyTextColor(colorId: Int, stickerView: StickerView, regretManager: RegretManager) {
        stickerView.currentSticker?.let {
            regretManager.setNewTextColor(colorId)
            (it as TextSticker).setTextColor(ContextCompat.getColor(context, colorId))
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyTextColor", "elvis", "Current Sticker is null")
        }
    }

    /* ------------------------------------------------- Bold ------------------------------------------------- */

    fun applyBoldTypeface(stickerView: StickerView, regretManager: RegretManager) {
        // Updating font style to Bold
        stickerView.currentSticker?.let {
            if (regretManager.isItalic) {
                if (regretManager.isBold)
                    (it as TextSticker).setTypefaceType(Typeface.ITALIC)
                else
                    (it as TextSticker).setTypefaceType(Typeface.BOLD_ITALIC)
            } else {
                if (regretManager.isBold)
                    (it as TextSticker).setTypefaceType(Typeface.NORMAL)
                else
                    (it as TextSticker).setTypefaceType(Typeface.BOLD)
            }
            regretManager.isBold = !regretManager.isBold
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyBoldTypeface", "elvis", "Current Sticker is null")
        }
    }

    /* ------------------------------------------------ Italic ------------------------------------------------- */

    fun applyItalicTypeface(stickerView: StickerView, regretManager: RegretManager) {
        // Updating font style to Italic
        stickerView.currentSticker?.let {
            if (regretManager.isBold) {
                if (regretManager.isItalic)
                    (it as TextSticker).setTypefaceType(Typeface.BOLD)
                else
                    (it as TextSticker).setTypefaceType(Typeface.BOLD_ITALIC)
            } else {
                if (regretManager.isItalic)
                    (it as TextSticker).setTypefaceType(Typeface.NORMAL)
                else
                    (it as TextSticker).setTypefaceType(Typeface.ITALIC)
            }
            regretManager.isItalic = !regretManager.isItalic
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyItalicTypeface", "elvis", "Current Sticker is null")
        }
    }

    /* ----------------------------------------------- Underline ------------------------------------------------ */

    fun applyUnderlineTypeface(stickerView: StickerView, regretManager: RegretManager) {
        // Underline the text
        stickerView.currentSticker?.let {
            val textSticker = it as TextSticker
            textSticker.setUnderline(!regretManager.isUnderline)
            regretManager.isUnderline = !regretManager.isUnderline
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyUnderlineTypeface", "elvis", "Current Sticker is null")
        }
    }

    fun applyStrikeThroughTypeface(stickerView: StickerView, regretManager: RegretManager) {
        // StrikeThrough the text
        stickerView.currentSticker?.let {
            val textSticker = it as TextSticker
            textSticker.setStrikeThrough(!regretManager.isStrikeThrough)
            regretManager.isStrikeThrough = !regretManager.isStrikeThrough
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyStrikeThroughTypeface", "elvis", "Current Sticker is null")
        }
    }

    fun applyTextOpacity(stickerView: StickerView, radius: Float) {
        // Text Opacity
        stickerView.currentSticker?.let {
            val textSticker = it as TextSticker
            textSticker.setAlpha((radius * 2.55).toInt())
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyTextOpacity", "elvis", "Current Sticker is null")
        }
    }

    fun applyTextShadow(stickerView: StickerView, radius: Float) {
        // Text Shadow
        stickerView.currentSticker?.let {
            val textSticker = it as TextSticker
            textSticker.setShadow(radius)
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyTextShadow", "elvis", "Current Sticker is null")
        }
    }

    fun applyTextBlur(stickerView: StickerView, radius: Float) {
        // Text Blurriness
        stickerView.currentSticker?.let {
            val textSticker = it as TextSticker
            textSticker.setBlur(radius)
            stickerView.invalidate()
        } ?: kotlin.run {
            //LogUtils.showLog(context, "applyTextBlur", "elvis", "Current Sticker is null")
        }
    }

}