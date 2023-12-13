package dev.pegasus.phototemplates.ui.activities

import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.raed.rasmview.brushtool.data.Brush
import com.raed.rasmview.brushtool.data.BrushesRepository
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.databinding.ActivityDrawBinding
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.ImageUtils
import dev.pegasus.template.viewModels.TemplateViewModel

class ActivityDraw : BaseActivity<ActivityDrawBinding>(R.layout.activity_draw) {

    private val rasmContext by lazy { binding?.rvBrushMain?.rasmContext }
    private var viewModel: TemplateViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        viewModel = mViewModel
        Log.d(TAG, "onCreate: activity draw viewModel instance ${viewModel.hashCode()}")

        binding?.mtbDraw?.title = resources.getString(R.string.draw_view)

        setBrushView()

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

        binding?.btnDone?.setOnClickListener {
            val drawingBitmap = rasmContext?.exportRasm()
            drawingBitmap?.let {
                viewModel?.saveBitmap(it)
            }
        }
    }

    private fun setBrushView() {

        viewModel?.getBitmap()?.let {
            Log.d("MyTag", "setBrushView: width: ${it.width} and height: ${it.height}")
            binding?.backgroundImage?.setImageBitmap(it)
        } ?: run {
            Log.d("MyTag", "setBrushView: bitmap is null")
        }

        rasmContext?.brushConfig = BrushesRepository(resources).get(Brush.Pen)
        rasmContext?.brushColor = Color.RED
        rasmContext?.rotationEnabled = true
        rasmContext?.setBackgroundColor(Color.TRANSPARENT)

        binding?.rvBrushMain?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                binding?.rvBrushMain?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                binding?.rvBrushMain?.resetTransformation()
            }
        })
    }

}