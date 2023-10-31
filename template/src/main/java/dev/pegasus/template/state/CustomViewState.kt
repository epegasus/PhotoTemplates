package dev.pegasus.template.state

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState

class CustomViewState : BaseSavedState {
    var imageBitmapUrix: Uri? = null
    var imageAspectRatiox: Float = 0f
    var scaleFactorx: Float = 1f
    var zoomCenterXx: Float = 0f
    var zoomCenterYx: Float = 0f
    var dxx: Float = 0f
    var dyx: Float = 0f

    constructor(superState: Parcelable?) : super(superState)

    constructor(parcel: Parcel) : super(parcel) {
        val uriString = parcel.readString()
        imageBitmapUrix = if (uriString != null) Uri.parse(uriString) else null
        imageAspectRatiox = parcel.readFloat()
        scaleFactorx = parcel.readFloat()
        zoomCenterXx = parcel.readFloat()
        zoomCenterYx = parcel.readFloat()
        dxx = parcel.readFloat()
        dyx = parcel.readFloat()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeString(imageBitmapUrix?.toString()) // Convert the Uri to a string
        out.writeParcelable(imageBitmapUrix, flags)
        out.writeFloat(imageAspectRatiox)
        out.writeFloat(scaleFactorx)
        out.writeFloat(zoomCenterXx)
        out.writeFloat(zoomCenterYx)
        out.writeFloat(dxx)
        out.writeFloat(dyx)
    }

    companion object CREATOR : Parcelable.Creator<CustomViewState> {
        override fun createFromParcel(parcel: Parcel): CustomViewState {
            return CustomViewState(parcel)
        }

        override fun newArray(size: Int): Array<CustomViewState?> {
            return arrayOfNulls(size)
        }
    }
}
