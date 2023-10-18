package dev.pegasus.phototemplates

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.template.utils.TemplateModel

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView(TemplateModel(1, R.drawable.img_bg_one, 1080f, 1354f, 513.35f, 822.85f, 487f, 264f))

        binding.btnChangeBackground.setOnClickListener {
            //binding.templateView.setBackgroundResource(R.drawable.img_bg_two)
            binding.view.isVisible = !binding.view.isVisible
        }

        binding.btnSelectPhoto.setOnClickListener { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
    }

    private fun initView(model: TemplateModel) {
        //binding.templateView.setBackgroundResource(R.drawable.img_bg_one)
        binding.templateView.setBackgroundFromModel(model)
        binding.templateView.setImageResource(R.drawable.img_pic)
    }

    // Initialize the galleryLauncher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let {
                this@MainActivity.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.templateView.setImageBitmap(bitmap)
                }
            }
        }
    }

}