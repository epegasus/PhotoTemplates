package dev.pegasus.phototemplates.helpers.di.modules

import dev.pegasus.template.utils.ImageUtils
import dev.pegasus.template.viewModels.TemplateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    single(createdAtStart = true) { TemplateViewModel() }

    factory { ImageUtils(context = this.androidContext()) }
}