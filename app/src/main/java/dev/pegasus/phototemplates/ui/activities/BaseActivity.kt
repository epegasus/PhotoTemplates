package dev.pegasus.phototemplates.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import dev.pegasus.phototemplates.helpers.di.components.DiComponent
import dev.pegasus.template.viewModels.TemplateViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseActivity<T: ViewDataBinding>(@LayoutRes layoutId: Int): AppCompatActivity() {

    protected val mBinding: T? by lazy {
        DataBindingUtil.inflate<T>(layoutInflater, layoutId, null, false)
    }

    protected val mViewModel: TemplateViewModel by viewModel()
    protected val diComponent by lazy { DiComponent() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable Full Screen and must be called before setContentView
        enableEdgeToEdge()

        // To get the status bar padding, we have
        // Also note that by using the below code,
        // our system bar or status bar will now have the same color is our main activity has
        mBinding?.root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it){ view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

    }

}