package dev.pegasus.phototemplates.commons.listeners

import dev.pegasus.template.dataClasses.TemplateModel

interface OnTemplateItemClickListener {
    fun onItemClick(model: TemplateModel)
}