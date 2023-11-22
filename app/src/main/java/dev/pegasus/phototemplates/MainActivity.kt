package dev.pegasus.phototemplates

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.viewModels.TemplateViewModel
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity(), ViewModelStoreOwner {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val dpTemplates by lazy { DpTemplates() }
    private lateinit var viewModel: TemplateViewModel
    private var mBitmap: Bitmap? = null

    // Initialize the galleryLauncher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let {
                this@MainActivity.contentResolver.openInputStream(it)?.use { inputStream ->
                    mBitmap = BitmapFactory.decodeStream(inputStream)
                    binding.templateView.setImageBitmap(mBitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TemplateViewModel::class.java]

        initView()

        binding.btnChangeBackground.setOnClickListener {
            binding.view.isGone = !binding.view.isGone
        }
        binding.btnSelectPhoto.setOnClickListener { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
    }

    private fun initView() {
        binding.templateView.setBackgroundFromModel(dpTemplates.list[1])
        binding.templateView.setImageResource(R.drawable.img_pic)
    }

}