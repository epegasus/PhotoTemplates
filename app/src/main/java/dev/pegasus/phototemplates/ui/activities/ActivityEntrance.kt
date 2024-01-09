package dev.pegasus.phototemplates.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.databinding.ActivityEntranceBinding

class ActivityEntrance : BaseActivity<ActivityEntranceBinding>(R.layout.activity_entrance) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding?.root)

        mBinding?.mbMainActivity?.setOnClickListener { startActivity(Intent(this@ActivityEntrance, MainActivity::class.java)) }
        mBinding?.mbTemplateActivity?.setOnClickListener { startActivity(Intent(this@ActivityEntrance, ActivityTemplate::class.java)) }
    }
}