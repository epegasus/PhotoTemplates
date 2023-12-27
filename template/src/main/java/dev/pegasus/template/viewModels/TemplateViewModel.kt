package dev.pegasus.template.viewModels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import dev.pegasus.template.utils.HelperUtils.TAG
import kotlin.math.log

class TemplateViewModel: ViewModel() {

    private var imageBitmap: Bitmap? = null

    // bitmap image which well be passed over to draw activity
    private var bitmap: Bitmap? = null

    fun updateImage(bitmap: Bitmap?){
        imageBitmap = bitmap
    }

    fun getImage(): Bitmap?{
        return imageBitmap
    }

    fun saveBitmap(bitmap: Bitmap?){
        bitmap?.let {
            Log.d(TAG, "saveBitmap: bitmap is not null and bitmap width: ${it.width} and height: ${it.height}")
            this.bitmap = bitmap
        } ?: run {
            Log.d(TAG, "saveBitmap: bitmap is null")
        }
    }

    fun getBitmap(): Bitmap? {
        Log.d(TAG, "getBitmap: bitmap width: ${bitmap?.width} and height: ${bitmap?.height}")
        return bitmap
    }

}