package dev.pegasus.template.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

class ImageUtils(private val context: Context) {

    fun createDrawableFromBitmap(bitmap: Bitmap): Drawable {
        return BitmapDrawable(context.resources, bitmap)
    }

    fun createBitmapFromDrawable(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun createTransparentBitmap(width: Int, height: Int): Bitmap {
        // Create a transparent bitmap with the specified width and height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create a Canvas to draw on the bitmap
        val canvas = Canvas(bitmap)

        // Draw a transparent color on the entire bitmap
        canvas.drawARGB(0, 0, 0, 0)

        return bitmap
    }
}