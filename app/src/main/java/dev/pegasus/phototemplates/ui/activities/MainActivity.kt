package dev.pegasus.phototemplates.ui.activities

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.snackbar.Snackbar
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.commons.listeners.OnTemplateItemClickListener
import dev.pegasus.phototemplates.commons.listeners.OnTextDoneClickListener
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.phototemplates.helpers.recyclerViews.TemplatesListAdapter
import dev.pegasus.phototemplates.ui.dialogs.DialogTextBox
import dev.pegasus.regret.RegretManager
import dev.pegasus.regret.enums.CaseType
import dev.pegasus.regret.interfaces.RegretListener
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.TextSticker
import dev.pegasus.stickers.helper.Sticker
import dev.pegasus.stickers.helper.events.DeleteIconEvent
import dev.pegasus.stickers.ui.BitmapStickerIcon
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.HelperUtils.isValidPosition
import dev.pegasus.template.viewModels.TemplateViewModel

class MainActivity : AppCompatActivity(), ViewModelStoreOwner, OnTemplateItemClickListener {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val dpTemplates by lazy { DpTemplates() }
    private lateinit var viewModel: TemplateViewModel
    private var mBitmap: Bitmap? = null
    private var dialogTextBox: DialogTextBox? = null
    private var templateAdapter: TemplatesListAdapter? = null

    // Regret Manager
    private val _regretManagerList = ArrayList<RegretManager>()
    private val regretManagerList: List<RegretManager> get() = _regretManagerList
    private var regretPosition = 0
    //private var isDialogActive = false

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
        initStickerView()
        initRecyclerView()

        binding.btnChangeBackground.setOnClickListener {
            binding.view.isGone = !binding.view.isGone
        }
        binding.btnSelectPhoto.setOnClickListener { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
        binding.btnAddSticker.setOnClickListener { showTextBoxDialog("") }
    }

    private fun initRecyclerView() {
        templateAdapter = TemplatesListAdapter(this)
        binding.templatesRecyclerView.adapter = templateAdapter
        templateAdapter?.submitList(dpTemplates.list)
    }

    private fun initView() {
        binding.templateView.setBackgroundFromModel(dpTemplates.list[0])
        binding.templateView.setImageResource(R.drawable.img_pic)
    }

    private fun initStickerView() {
        val deleteIcon = BitmapStickerIcon(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_close), BitmapStickerIcon.LEFT_TOP)
        deleteIcon.iconEvent = DeleteIconEvent()

        binding.stickerView.icons = arrayListOf(deleteIcon)
        binding.stickerView.isLocked = false
        binding.stickerView.isConstrained = true

        setStickerViewListener()
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

    private fun addItemRegretManager(sticker: TextSticker) {
        _regretManagerList.apply {
            add(
                RegretManager(
                    this@MainActivity,
                    object : RegretListener {
                        override fun onDo(key: CaseType, value: Any?) {}
                        override fun onCanDo(canUndo: Boolean, canRedo: Boolean) {}
                    },
                )
            )
        }
        regretPosition = regretManagerList.size - 1
        if (regretPosition.isValidPosition(regretManagerList)) {
             regretManagerList[regretPosition].setView(sticker)
        }
    }

    private fun setStickerViewListener() {
        binding.stickerView.onStickerOperationListener = object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                Log.d("TAG", "onStickerAdded")
                addItemRegretManager(sticker as TextSticker)
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextSticker) {
                    regretManagerList.forEachIndexed let@ { index, regretManager ->
                        if (regretManager.getView() == sticker){
                            regretPosition = index
                            return@let
                        }
                    }
                    sticker.setTextColor(Color.RED)
                    binding.stickerView.replace(sticker)
                    binding.stickerView.invalidate()
                }
                Log.d("TAG", "onStickerClicked")
            }

            override fun onStickerDeleted(sticker: Sticker) {
                Log.d("TAG", "onStickerDeleted")
                if (regretManagerList.isNotEmpty() && regretManagerList.size > regretPosition){
                    _regretManagerList.removeAt(regretPosition)
                }
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

    override fun onItemClick(model: TemplateModel) {
        Log.d(TAG, "onItemClick: model: $model")
        binding.templateView.setBackgroundFromModel(model)
    }

}