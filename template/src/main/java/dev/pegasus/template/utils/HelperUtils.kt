package dev.pegasus.template.utils

import androidx.recyclerview.widget.RecyclerView

object HelperUtils {

    const val TAG = "MyTag"

    fun Int.isValidPosition(list: List<Any>): Boolean {
        return this != RecyclerView.NO_POSITION && this < list.size && list.isNotEmpty()
    }

}