package dev.pegasus.phototemplates

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import dev.pegasus.phototemplates.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

        binding.btnChangeBackground.setOnClickListener {
            //binding.templateView.setBackgroundResource(R.drawable.img_bg_two)
            binding.view.isVisible = !binding.view.isVisible
        }
    }

    private fun initView() {
        binding.templateView.setBackgroundResource(R.drawable.img_bg_one)
        binding.templateView.setImageResource(R.drawable.img_pic)
    }
}