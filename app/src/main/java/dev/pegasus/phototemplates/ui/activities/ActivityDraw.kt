package dev.pegasus.phototemplates.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import com.raed.rasmview.brushtool.data.Brush
import com.raed.rasmview.brushtool.data.BrushesRepository
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.databinding.ActivityDrawBinding
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.viewModels.TemplateViewModel

class ActivityDraw : BaseActivity<ActivityDrawBinding>(R.layout.activity_draw) {

    private val rasmContext by lazy { mBinding?.rvBrushMain?.rasmContext }
    private var viewModel: TemplateViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding?.root)

        viewModel = mViewModel
        Log.d(TAG, "onCreate: activity draw viewModel instance ${viewModel.hashCode()}")

        mBinding?.mtbDraw?.title = resources.getString(R.string.draw_view)

        setBrushView()

        mBinding?.sliderMain?.addOnChangeListener { _, value, _ ->
            mBinding?.tvValueMain?.text = value.toInt().toString()
            rasmContext?.brushConfig?.size = value / 100
        }

        mBinding?.ifvUndoMain?.setOnClickListener {
            with(rasmContext?.state){
                if (this?.canCallUndo() == true) undo()
            }
        }

        mBinding?.ifvRedoMain?.setOnClickListener {
            with(rasmContext?.state){
                if (this?.canCallRedo() == true) redo()
            }
        }

        mBinding?.btnDone?.setOnClickListener {
            val drawingBitmap = rasmContext?.exportRasm()
            drawingBitmap?.let {
                viewModel?.saveBitmap(it)
            }
        }
    }

    private fun setBrushView() {

        viewModel?.getBitmap()?.let {
            Log.d("MyTag", "setBrushView: width: ${it.width} and height: ${it.height}")
            mBinding?.backgroundImage?.setImageBitmap(it)
        } ?: run {
            Log.d("MyTag", "setBrushView: bitmap is null")
        }

        rasmContext?.brushConfig = BrushesRepository(resources).get(Brush.Pen)
        rasmContext?.brushColor = Color.RED
        rasmContext?.rotationEnabled = true
        rasmContext?.setBackgroundColor(Color.TRANSPARENT)

        mBinding?.rvBrushMain?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                mBinding?.rvBrushMain?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                mBinding?.rvBrushMain?.resetTransformation()
            }
        })
    }

}