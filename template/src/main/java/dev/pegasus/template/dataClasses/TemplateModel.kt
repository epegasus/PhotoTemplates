package dev.pegasus.template.dataClasses

data class TemplateModel(
    val id: Int,
    val templateType: TemplateType,
    val frameType: FrameType,
    val thumbnailImage: Int,
    val bgImage: Int,
    val template: Int,
    val width: Float,
    val height: Float,
    val frameWidth: Float,
    val frameHeight: Float,
    val frameX: Float,
    val frameY: Float
)
