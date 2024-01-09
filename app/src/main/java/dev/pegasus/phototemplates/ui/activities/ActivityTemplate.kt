package dev.pegasus.phototemplates.ui.activities

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.databinding.ActivityTemplateBinding

class ActivityTemplate : BaseActivity<ActivityTemplateBinding>(R.layout.activity_template) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding?.root)
    }

}