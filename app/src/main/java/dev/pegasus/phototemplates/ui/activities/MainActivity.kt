package dev.pegasus.phototemplates.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.raed.rasmview.RasmContext
import com.raed.rasmview.brushtool.data.Brush
import com.raed.rasmview.brushtool.data.BrushesRepository
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.commons.dataProvider.TextStickerDataProvider
import dev.pegasus.phototemplates.commons.listeners.OnTemplateItemClickListener
import dev.pegasus.phototemplates.commons.listeners.OnTextDoneClickListener
import dev.pegasus.phototemplates.databinding.ActivityMainBinding
import dev.pegasus.phototemplates.databinding.MainControlsAndTemplatesLayoutBinding
import dev.pegasus.phototemplates.databinding.TextStickerControlsLayoutBinding
import dev.pegasus.phototemplates.helpers.adapters.TemplatesListAdapter
import dev.pegasus.phototemplates.helpers.adapters.TextStickerListAdapter
import dev.pegasus.phototemplates.helpers.model.TextStickerModel
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
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main), ViewModelStoreOwner, OnTemplateItemClickListener {

    private val dpTemplates by lazy { DpTemplates() }
    private var viewModel: TemplateViewModel? = null
    private var mBitmap: Bitmap? = null
    private var dialogTextBox: DialogTextBox? = null
    private var templateAdapter: TemplatesListAdapter? = null

    private val inflater by lazy { LayoutInflater.from(this) }
    private var layoutToAdd: View? = null

    // for the text sticker controls
    private var textStickerAdapter: TextStickerListAdapter? = null
    private val textStickerList by lazy { TextStickerDataProvider() }

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
                    mBinding?.templateView?.setImageBitmap(mBitmap)
                }
            }
        }
    }

    companion object {
        private var SELECTED_STICKER_POSITION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding?.root)

        viewModel = mViewModel

        Log.d(TAG, "onCreate: main activity viewModel instance ${viewModel.hashCode()}")

        initMainControlsView()
        initStickerView()

        mBinding?.btnDone?.setOnClickListener {
            mBinding?.apply {

                val bitmap = rasmContext?.exportRasm()
                bitmap?.let {
                    Log.d(TAG, "onCreate: received bitmap width: ${it.width} and height: ${it.height}")
                }

                btnDone.visibility = View.GONE
                rvBrushMain.visibility = View.GONE
                sliderMain.visibility = View.GONE
                ifvUndoMain.visibility = View.GONE
                ifvRedoMain.visibility = View.GONE
            }
        }

        mBinding?.sliderMain?.addOnChangeListener { _, value, _ ->
            mBinding?.tvValueMain?.text = value.toInt().toString()
            rasmContext?.brushConfig?.size = value / 100
        }

        mBinding?.ifvUndoMain?.setOnClickListener {
            with(rasmContext?.state) {
                if (this?.canCallUndo() == true) undo()
            }
        }

        mBinding?.ifvRedoMain?.setOnClickListener {
            with(rasmContext?.state) {
                if (this?.canCallRedo() == true) redo()
            }
        }

        mBinding?.mtbMain?.title = resources.getString(R.string.template_view)
    }

    private fun initMainControlsView() {
        mBinding?.templateView?.setBackgroundFromModel(dpTemplates.list[0])
        mBinding?.templateView?.setImageResource(R.drawable.img_pic)

        // Inflate the main control layout
        val binding: MainControlsAndTemplatesLayoutBinding? = DataBindingUtil.inflate(
            inflater,
            R.layout.main_controls_and_templates_layout,
            mBinding?.flContainer,
            false
        )
        layoutToAdd = binding?.root

        // Add the layout to the FrameLayout
        mBinding?.flContainer?.addView(layoutToAdd)

        val recyclerView = binding?.templatesRecyclerView
        templateAdapter = TemplatesListAdapter(this)
        recyclerView?.adapter = templateAdapter
        templateAdapter?.submitList(dpTemplates.list)

        binding?.btnAddTextSticker?.setOnClickListener { initTextStickerControlsView() }
        binding?.btnAddEmojiSticker?.setOnClickListener {
            val drawableSticker = ContextCompat.getDrawable(this@MainActivity, dev.pegasus.stickers.R.drawable.ic_haha_emoji)
            drawableSticker?.let {
                if (it.intrinsicWidth > 0 && it.intrinsicHeight > 0) {
                    val emojiSticker = DrawableSticker(it)
                    mBinding?.stickerView?.addSticker(emojiSticker)
                }
            }
        }
        binding?.btnDraw?.setOnClickListener {  }
        binding?.btnChangeBackground?.setOnClickListener { mBinding?.view?.isGone = !mBinding?.view?.isGone!! }
        binding?.btnSelectPhoto?.setOnClickListener { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
    }

    private fun initTextStickerControlsView() {
        val binding: TextStickerControlsLayoutBinding? = DataBindingUtil.inflate(inflater, R.layout.text_sticker_controls_layout, mBinding?.flContainer, false)
        layoutToAdd = binding?.root
        // First, let's remove all child views
        mBinding?.flContainer?.removeAllViews()
        mBinding?.flContainer?.addView(layoutToAdd)

        textStickerAdapter = TextStickerListAdapter(
            itemClick = { model: TextStickerModel, position: Int ->
                regretManagerList.forEachIndexed let@{ index, regretManager ->
                    if (regretManager.getView()?.text == model.text) {
                        Log.d(TAG, "initTextStickerControlsView: text matched")
                        regretPosition = index
                        return@let
                    }
                }
                addSticker(model.text)
            },
            addTextStickerButtonClick = { position ->
                showTextBoxDialog()
            },
            handleStickerClick = { position ->
                lifecycleScope.launch {
                    if (position != SELECTED_STICKER_POSITION){
                        textStickerList.list[SELECTED_STICKER_POSITION].isSelected = false
                        SELECTED_STICKER_POSITION = position
                        textStickerList.list[SELECTED_STICKER_POSITION].isSelected = true
                        // Only submitting the list to adapter is not working properly,
                        // you have to reassign the adapter to recyclerview too
                        binding?.fontsRecyclerView?.adapter = textStickerAdapter
                        textStickerAdapter?.submitList(textStickerList.list)
                    }
                }
            })
        binding?.fontsRecyclerView?.adapter = textStickerAdapter
        textStickerAdapter?.submitList(textStickerList.list)

        binding?.btnCross?.setOnClickListener {
            mBinding?.flContainer?.removeAllViews()
            initMainControlsView()
        }
        binding?.btnDone?.setOnClickListener {
            mBinding?.flContainer?.removeAllViews()
            initMainControlsView()
        }

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
                    dialogTextBox?.dismiss()
                }
            })
            it.show(supportFragmentManager, "dialog_text_box")
        }
    }

    private fun updateSticker(text: String) {
        mBinding?.stickerView?.currentSticker?.let {
            (it as TextSticker).text = text
            it.resizeText()
            mBinding?.stickerView?.invalidate()
        }
    }

    private fun addSticker(newText: String) {
        val sticker = TextSticker(this@MainActivity)
        sticker.text = newText
        sticker.setTextColor(ContextCompat.getColor(this@MainActivity, dev.pegasus.stickers.R.color.purple_200))
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
        sticker.resizeText()
        mBinding?.stickerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mBinding?.stickerView?.viewTreeObserver!!.removeOnGlobalLayoutListener(this)
                if (mBinding?.stickerView?.stickerCount!! < 20) {
                    mBinding?.stickerView?.addSticker(sticker)
                } else mBinding?.root?.let { Snackbar.make(it, resources.getString(R.string.limit_reached), Snackbar.LENGTH_LONG).show() }
            }
        })
    }

    private fun addItemRegretManager(sticker: TextSticker) {
        _regretManagerList.apply {
            add(
                RegretManager(
                    this@MainActivity,
                    object : RegretListener {
                        override fun onDo(key: CaseType, value: Any?, regretType: Int) {}
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
        mBinding?.stickerView?.onStickerOperationListener = object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                Log.d("TAG", "onStickerAdded")
                if (sticker is TextSticker) addItemRegretManager(sticker)
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextSticker) {
                    regretManagerList.forEachIndexed let@{ index, regretManager ->
                        if (regretManager.getView() == sticker) {
                            regretPosition = index
                            return@let
                        }
                    }
                }
                Log.d("TAG", "onStickerClicked")
            }

            override fun onStickerDeleted(sticker: Sticker) {
                Log.d("TAG", "onStickerDeleted")
                if (regretManagerList.isNotEmpty() && regretManagerList.size > regretPosition) {
                    _regretManagerList.removeAt(regretPosition)
                }
            }

            override fun onStickerTouchedDown(sticker: Sticker, isUpdate: Boolean, isDuplicate: Boolean) {
                if (!isUpdate) return
                if (sticker is TextSticker) {
                    showTextBoxDialog(sticker.text)
                }
                Log.d("TAG", "onStickerTouchedDown")
            }

            override fun onStickerDragFinished(sticker: Sticker) {}
            override fun onStickerZoomFinished(sticker: Sticker) {}
            override fun onStickerFlipped(sticker: Sticker) {}
            override fun onStickerDoubleTapped(sticker: Sticker) {}
            override fun onStickerUpdated(sticker: Sticker) {}
        }

    }

    override fun onItemClick(model: TemplateModel) {
        Log.d(TAG, "onItemClick: model: $model")
        mBinding?.templateView?.setBackgroundFromModel(model)
    }

    private fun setBrushView() {
        rasmContext = if (mBinding?.rvBrushMain?.isVisible == true) mBinding?.rvBrushMain?.rasmContext
        else null

        rasmContext?.let {
            mBinding?.rvBrushMain?.rasmContext?.brushConfig = BrushesRepository(resources).get(Brush.Pen)
            mBinding?.rvBrushMain?.rasmContext?.brushColor = Color.RED
            mBinding?.rvBrushMain?.rasmContext?.rotationEnabled = true
            mBinding?.rvBrushMain?.rasmContext?.setBackgroundColor(Color.TRANSPARENT)

            mBinding?.rvBrushMain?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mBinding?.rvBrushMain?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    mBinding?.rvBrushMain?.resetTransformation()
                }
            })
        }

    }

}