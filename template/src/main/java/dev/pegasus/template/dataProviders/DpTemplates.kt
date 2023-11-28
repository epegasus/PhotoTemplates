package dev.pegasus.template.dataProviders

import dev.pegasus.template.R
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.dataClasses.FrameType
import dev.pegasus.template.dataClasses.TemplateType

class DpTemplates {

    // 100 templates by designers will be as follow
    val list = listOf(
        TemplateModel(id = 1, templateType = TemplateType.Portrait, frameType = FrameType.Rectangle, thumbnailImage = R.drawable.img_bg_c1_thumbnail_1, bgImage = R.drawable.img_bg_c1_template_1, width = 1080F, height = 1350F, frameWidth = 418F, frameHeight = 680F, frameX = 613F, frameY = 322F),
        TemplateModel(id = 2, templateType = TemplateType.Portrait, frameType = FrameType.Circle, thumbnailImage = R.drawable.img_bg_c2_thumbnail_1, bgImage = R.drawable.img_bg_c2_template_1, width = 1080F, height = 1350F, frameWidth = 778F, frameHeight = 778F, frameX = 151F, frameY = 78F),
        TemplateModel(id = 3, templateType = TemplateType.Landscape, frameType = FrameType.Rectangle, thumbnailImage = R.drawable.img_bg_c1_thumbnail_2, bgImage = R.drawable.img_bg_c1_template_2, width = 1080F, height = 680F, frameWidth = 298F, frameHeight = 420F, frameX = 720F, frameY = 94F),
        TemplateModel(id = 4, templateType = TemplateType.Square, frameType = FrameType.Rectangle, thumbnailImage = R.drawable.img_bg_c1_thumbnail_3, bgImage = R.drawable.img_bg_c1_template_3, width = 1080F, height = 1080F, frameWidth = 563.35F, frameHeight = 686.32F, frameX = 258F, frameY = 376F)
    )

}