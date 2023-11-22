package dev.pegasus.template.dataProviders

import dev.pegasus.template.R
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.dataClasses.TemplateType

class DpTemplates {

    // 100 templates by designers will be as follow
    val list = listOf(
        TemplateModel(id = 1, frameType = TemplateType.Rectangle, thumbnailImage = R.drawable.img_bg_c1_thumbnail_1, bgImage = R.drawable.img_bg_c1_template_1, width = 1080F, height = 1350F, frameWidth = 418F, frameHeight = 680F, frameX = 613F, frameY = 322F),
        TemplateModel(id = 2, frameType = TemplateType.Circle, thumbnailImage = R.drawable.img_bg_c2_thumbnail_1, bgImage = R.drawable.img_bg_c2_template_1, width = 1080F, height = 1350F, frameWidth = 778F, frameHeight = 778F, frameX = 151F, frameY = 78F)
    )

}