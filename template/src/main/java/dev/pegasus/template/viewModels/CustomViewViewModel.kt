package dev.pegasus.template.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel

class CustomViewViewModel: ViewModel() {

    private var imageBitmap: Bitmap? = null

    fun updateImage(bitmap: Bitmap?){
        imageBitmap = bitmap
    }

    fun getImage(): Bitmap?{
        return imageBitmap
    }

}