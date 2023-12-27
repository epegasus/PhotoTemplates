package dev.pegasus.phototemplates.helpers.di.components

import dev.pegasus.template.utils.ImageUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DiComponent: KoinComponent {
    val imageUtil by inject<ImageUtils>()
}