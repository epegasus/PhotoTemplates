package dev.pegasus.stickers.helper.events

import android.util.Log
import android.view.MotionEvent
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.ui.StickerIconEvent

/**
 * @Author: SOHAIB AHMED
 * @Date: 22-12-2023
 * @Accounts
 *      -> https://github.com/epegasus
 *      -> https://stackoverflow.com/users/20440272/sohaib-ahmed
 */

class DuplicateIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionUp(stickerView: StickerView, event: MotionEvent?) {
        /*val currentSticker = stickerView.currentSticker
        currentSticker?.let {
            stickerView.addSticker(currentSticker)
        } ?: run {
            Log.e("MyTag", "StickerView: DuplicateIconEvent: onActionUp: ", NullPointerException("Current Sticker is null"))
        }*/
    }
}

