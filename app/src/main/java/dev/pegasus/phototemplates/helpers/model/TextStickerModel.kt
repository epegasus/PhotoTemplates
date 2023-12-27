package dev.pegasus.phototemplates.helpers.model

data class TextStickerModel(
    var id: Int,
    var text: String,
    var fontSize: Int,
    var fontWeight: String,
    var fontStyle: String,
    var isSelected: Boolean = false
)
