package dev.pegasus.phototemplates.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.snackbar.Snackbar
import com.raed.rasmview.RasmContext
import com.raed.rasmview.brushtool.data.Brush
import com.raed.rasmview.brushtool.data.BrushesRepository
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.commons.listeners.OnTemplateItemClickListener
import dev.pegasus.phototemplates.commons.listeners.OnTextDoneClickListener
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.phototemplates.helpers.adapters.TemplatesListAdapter
import dev.pegasus.phototemplates.ui.dialogs.DialogTextBox
import dev.pegasus.regret.RegretManager
import dev.pegasus.regret.enums.CaseType
import dev.pegasus.regret.interfaces.RegretListener
import dev.pegasus.stickers.StickerView
import dev.pegasus.stickers.TextSticker
import dev.pegasus.stickers.helper.Sticker
import dev.pegasus.stickers.ui.DrawableSticker
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.dataProviders.DpTemplates
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.HelperUtils.isValidPosition
import dev.pegasus.template.viewModels.TemplateViewModel

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main), ViewModelStoreOwner, OnTemplateItemClickListener {

    private val dpTemplates by lazy { DpTemplates() }
    private var viewModel: TemplateViewModel? = null
    private var mBitmap: Bitmap? = null
    private var dialogTextBox: DialogTextBox? = null
    private var templateAdapter: TemplatesListAdapter? = null

    private var rasmContext: RasmContext? = null

    // Regret Manager
    private val _regretManagerList = ArrayList<RegretManager>()
    private val regretManagerList: List<RegretManager> get() = _regretManagerList
    private var regretPosition = 0

    // Add this property to satisfy the ViewModelStoreOwner interface
    override val viewModelStore: ViewModelStore
        get() = ViewModelStore()

    // Initialize the galleryLauncher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let {
                this@MainActivity.contentResolver.openInputStream(it)?.use { inputStream ->
                    mBitmap = BitmapFactory.decodeStream(inputStream)
                    binding?.templateView?.setImageBitmap(mBitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        viewModel = mViewModel

        Log.d(TAG, "onCreate: main activity viewModel instance ${viewModel.hashCode()}")

        initView()
        initStickerView()
        initRecyclerView()

        binding?.btnChangeBackground?.setOnClickListener {
            binding?.apply {
                btnDone.visibility = View.VISIBLE
                rvBrushMain.visibility = View.VISIBLE
                sliderMain.visibility = View.VISIBLE
                ifvUndoMain.visibility = View.VISIBLE
                ifvRedoMain.visibility = View.VISIBLE
                templatesRecyclerView.visibility = View.GONE
                setBrushView()
            }

            //binding.view.isGone = !binding.view.isGone
//            viewModel?.saveBitmap(binding?.templateView?.getCanvasBitmap())
//            startActivity(Intent(this@MainActivity, ActivityDraw::class.java))
        }
        binding?.btnDone?.setOnClickListener {
            binding?.apply {

                val bitmap = rasmContext?.exportRasm()
                bitmap?.let {
                    Log.d(TAG, "onCreate: received bitmap width: ${it.width} and height: ${it.height}")
                    templateView.savePaintBitmap(it)
                }

                btnDone.visibility = View.GONE
                rvBrushMain.visibility = View.GONE
                sliderMain.visibility = View.GONE
                ifvUndoMain.visibility = View.GONE
                ifvRedoMain.visibility = View.GONE
                templatesRecyclerView.visibility = View.VISIBLE
            }
        }

        binding?.sliderMain?.addOnChangeListener { _, value, _ ->
            binding?.tvValueMain?.text = value.toInt().toString()
            rasmContext?.brushConfig?.size = value / 100
        }

        binding?.ifvUndoMain?.setOnClickListener {
            with(rasmContext?.state){
                if (this?.canCallUndo() == true) undo()
            }
        }

        binding?.ifvRedoMain?.setOnClickListener {
            with(rasmContext?.state){
                if (this?.canCallRedo() == true) redo()
            }
        }

        binding?.btnSelectPhoto?.setOnClickListener { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
        binding?.btnAddSticker?.setOnClickListener { showTextBoxDialog() }

        //binding?.zoomWithFlingView?.setImageBitmap(R.drawable.img_pic)
        //binding?.zoomWithVelocityTracker?.setImageResource(R.drawable.img_pic)

        binding?.mtbMain?.title = resources.getString(R.string.template_view)
    }

    private fun initRecyclerView() {
        templateAdapter = TemplatesListAdapter(this)
        binding?.templatesRecyclerView?.adapter = templateAdapter
        templateAdapter?.submitList(dpTemplates.list)
    }

    private fun initView() {
        binding?.templateView?.setBackgroundFromModel(dpTemplates.list[0])
        binding?.templateView?.setImageResource(R.drawable.img_pic)
    }

    private fun initStickerView() {
        setStickerViewListener()
    }

    private fun showTextBoxDialog(text: String? = "") {
        dialogTextBox?.dismiss()
        dialogTextBox = DialogTextBox.newInstance(text)
        dialogTextBox?.let {
            it.setListener(object : OnTextDoneClickListener {
                override fun onDoneText(text: String, isUpdate: Boolean) {
                    if (isUpdate) updateSticker(text)
                    else addSticker(text)
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

    private fun updateSticker(text: String) {
        binding?.stickerView?.currentSticker?.let {
            (it as TextSticker).text = text
            it.resizeText()
            binding?.stickerView?.invalidate()
        }
    }

    private fun addSticker(newText: String) {
        val sticker = TextSticker(this@MainActivity)
        sticker.text = newText
        sticker.setTextColor(ContextCompat.getColor(this@MainActivity, dev.pegasus.stickers.R.color.purple_200))
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
        sticker.resizeText()
        binding?.stickerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding?.stickerView?.viewTreeObserver!!.removeOnGlobalLayoutListener(this)
                if (binding?.stickerView?.stickerCount!! < 20) {
                    binding?.stickerView?.addSticker(sticker)
                } else binding?.root?.let { Snackbar.make(it, resources.getString(R.string.limit_reached), Snackbar.LENGTH_LONG).show() }
            }
        })

        val drawableSticker = ContextCompat.getDrawable(this@MainActivity, dev.pegasus.stickers.R.drawable.ic_haha_emoji)
        drawableSticker?.let {
            if (drawableSticker.intrinsicWidth > 0 && drawableSticker.intrinsicHeight > 0){
                val emojiSticker = DrawableSticker(it)
                binding?.stickerView?.addSticker(emojiSticker)
            }
        }
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
        binding?.stickerView?.onStickerOperationListener = object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                Log.d("TAG", "onStickerAdded")
                if (sticker is TextSticker) addItemRegretManager(sticker)
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextSticker) {
                    regretManagerList.forEachIndexed let@ { index, regretManager ->
                        if (regretManager.getView() == sticker){
                            regretPosition = index
                            return@let
                        }
                    }
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

            override fun onStickerTouchedDown(sticker: Sticker, isUpdate: Boolean) {
                if (!isUpdate) return
                if (sticker is TextSticker){
                    showTextBoxDialog(sticker.text)
                }
                Log.d("TAG", "onStickerTouchedDown")
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                Log.d("TAG", "onStickerZoomFinished")
            }

            override fun onStickerDoubleTapped(sticker: Sticker) {
                Log.d("TAG", "onDoubleTapped: double tap will be with two click")
            }
        }
    }

    override fun onItemClick(model: TemplateModel) {
        Log.d(TAG, "onItemClick: model: $model")
        binding?.templateView?.setBackgroundFromModel(model)
    }

    private fun setBrushView() {
        rasmContext = if (binding?.rvBrushMain?.isVisible == true) binding?.rvBrushMain?.rasmContext
        else null

        rasmContext?.let {
            binding?.rvBrushMain?.rasmContext?.brushConfig = BrushesRepository(resources).get(Brush.Pen)
            binding?.rvBrushMain?.rasmContext?.brushColor = Color.RED
            binding?.rvBrushMain?.rasmContext?.rotationEnabled = true
            binding?.rvBrushMain?.rasmContext?.setBackgroundColor(Color.TRANSPARENT)

            binding?.rvBrushMain?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    binding?.rvBrushMain?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    binding?.rvBrushMain?.resetTransformation()
                }
            })
        }

    }

}