package dev.pegasus.phototemplates

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.snackbar.Snackbar
import dev.pegasus.phototemplates.clickListeners.OnTextDoneClickListener
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.phototemplates.dialogs.DialogTextBox
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.TextSticker
import dev.pegasus.stickers.helper.Sticker
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.viewModels.TemplateViewModel
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity(), ViewModelStoreOwner {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val dpTemplates by lazy { DpTemplates() }
    private lateinit var viewModel: TemplateViewModel
    private var mBitmap: Bitmap? = null
    private var dialogTextBox: DialogTextBox? = null

    // Add this property to satisfy the ViewModelStoreOwner interface
    override val viewModelStore: ViewModelStore
        get() = ViewModelStore()

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
        binding.btnAddSticker.setOnClickListener { showTextBoxDialog("") }
        setStickerViewListener()
    }

    private fun initView() {
        binding.templateView.setBackgroundFromModel(dpTemplates.list[1])
        binding.templateView.setImageResource(R.drawable.img_pic)
    }

    private fun showTextBoxDialog(text: String?) {
        dialogTextBox?.dismiss()
        dialogTextBox = DialogTextBox.newInstance(text)
        dialogTextBox?.let {
            it.setListener(object : OnTextDoneClickListener {
                override fun onDoneText(text: String, isUpdate: Boolean) {
                    /*if (isUpdate)
                        //updateSticker(text)
                    else*/
                        addSticker(text)
                }

                override fun onCancelText() {
//                    if (regretManagerList.isEmpty())
//                        onBackPress()
                    dialogTextBox?.dismiss()
                }
            })
            it.show(supportFragmentManager, "dialog_text_box")
        }
    }

    /*private fun updateSticker(newText: String) {
        binding.stickerView.currentSticker?.let {
            (it as TextSticker).text = newText
            it.resizeText()
            binding.stickerView.invalidate()
            applyNewText(newText)
        }
    }*/

    private fun addSticker(newText: String) {
        val sticker = TextSticker(this@MainActivity)
        sticker.text = newText
        sticker.setTextColor(ContextCompat.getColor(this@MainActivity, dev.pegasus.stickers.R.color.purple_200))
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
        sticker.resizeText()
        binding.stickerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.stickerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (binding.stickerView.stickerCount < 20) {
                    binding.stickerView.addSticker(sticker)
                } else Snackbar.make(binding.root, resources.getString(R.string.limit_reached), Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun setStickerViewListener() {
        binding.stickerView.onStickerOperationListener = object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                Log.d("TAG", "onStickerAdded")
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextSticker) {
                    sticker.setTextColor(Color.RED)
                    binding.stickerView.replace(sticker)
                    binding.stickerView.invalidate()
                }
                Log.d("TAG", "onStickerClicked")
            }

            override fun onStickerDeleted(sticker: Sticker) {
                Log.d("TAG", "onStickerDeleted")
            }

            override fun onStickerDragFinished(sticker: Sticker) {
                Log.d("TAG", "onStickerDragFinished")
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                Log.d("TAG", "onStickerTouchedDown")
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                Log.d("TAG", "onStickerZoomFinished")
            }

            override fun onStickerFlipped(sticker: Sticker) {
                Log.d("TAG", "onStickerFlipped")
            }

            override fun onStickerDoubleTapped(sticker: Sticker) {
                Log.d("TAG", "onDoubleTapped: double tap will be with two click")
            }
        }
    }

}