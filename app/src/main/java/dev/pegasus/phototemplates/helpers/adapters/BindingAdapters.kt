package dev.pegasus.phototemplates.helpers.adapters

import android.view.View
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.commons.listeners.RapidSafeListener.setOnRapidClickSafeListener

@BindingAdapter("templateThumbnail")
fun ImageFilterView.setImage(imageId: Int) {
    Glide.with(this).load(imageId).placeholder(R.drawable.img_pic).into(this)
}

@BindingAdapter("rapidSafeClick")
fun setOnRapidSafeClick(view: View, rapidSafeClick: () -> Unit) {
    view.setOnRapidClickSafeListener {
        rapidSafeClick.invoke()
    }
}