package dev.pegasus.template.state

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState

class CustomViewState : BaseSavedState {
    var imageAspectRatio: Float = 0f
    var scaleFactor: Float = 1f
    var zoomCenterX: Float = 0f
    var zoomCenterY: Float = 0f
    var dx: Float = 0f
    var dy: Float = 0f

    constructor(superState: Parcelable?) : super(superState)

    constructor(parcel: Parcel) : super(parcel) {
        imageAspectRatio = parcel.readFloat()
        scaleFactor = parcel.readFloat()
        zoomCenterX = parcel.readFloat()
        zoomCenterY = parcel.readFloat()
        dx = parcel.readFloat()
        dy = parcel.readFloat()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeFloat(imageAspectRatio)
        out.writeFloat(scaleFactor)
        out.writeFloat(zoomCenterX)
        out.writeFloat(zoomCenterY)
        out.writeFloat(dx)
        out.writeFloat(dy)
    }

}
