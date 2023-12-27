package dev.pegasus.phototemplates.helpers.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.databinding.ItemAddTextStickerLayoutBinding
import dev.pegasus.phototemplates.databinding.ItemTextStickerRowLayoutBinding
import dev.pegasus.phototemplates.helpers.model.TextStickerModel
import java.lang.IllegalArgumentException

class TextStickerListAdapter (
    private val itemClick: ( model :TextStickerModel, position: Int) -> Unit,
    private val addTextStickerButtonClick: (position: Int) -> Unit,
    private val handleStickerClick: (position: Int) -> Unit
) : ListAdapter<TextStickerModel, RecyclerView.ViewHolder>(diffUtil) {

    override fun getItemViewType(position: Int): Int {
        return if (position == SPECIAL_POSITION) VIEW_TYPE_SPECIAL else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(viewType){
            VIEW_TYPE_NORMAL -> {
                val binding = DataBindingUtil.inflate<ItemTextStickerRowLayoutBinding>(layoutInflater, R.layout.item_text_sticker_row_layout, parent, false)
                TextStickerViewHolder(binding)
            }
            VIEW_TYPE_SPECIAL -> {
                val binding = DataBindingUtil.inflate<ItemAddTextStickerLayoutBinding>(layoutInflater, R.layout.item_add_text_sticker_layout, parent, false)
                AddTextStickerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is TextStickerViewHolder -> {
                val currentItem = getItem(position)
                holder.binding.model = currentItem
                holder.binding.ifvEdit.setOnClickListener {
                    itemClick.invoke(currentItem, position)
                }

                holder.binding.ifvEdit.visibility = if (currentItem.isSelected) View.VISIBLE else View.GONE
                holder.binding.containerViewText.isSelected = currentItem.isSelected

                holder.binding.containerViewText.setOnClickListener {
                    handleStickerClick.invoke(position)
                    itemClick.invoke(currentItem, position)
                }
            }
            is AddTextStickerViewHolder -> { holder.binding.containerViewAdd.setOnClickListener {
                handleStickerClick.invoke(position)
                addTextStickerButtonClick.invoke(position) }
            }
        }
    }

    // For sticker text layout
    inner class TextStickerViewHolder(val binding: ItemTextStickerRowLayoutBinding) : RecyclerView.ViewHolder(binding.root)
    // For add text sticker button layout
    inner class AddTextStickerViewHolder(val binding: ItemAddTextStickerLayoutBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        const val SPECIAL_POSITION = 0  // the actual position where we want the special layout
        const val VIEW_TYPE_SPECIAL = 1
        const val VIEW_TYPE_NORMAL = 2

        val diffUtil = object : DiffUtil.ItemCallback<TextStickerModel>() {
            override fun areItemsTheSame(oldItem: TextStickerModel, newItem: TextStickerModel): Boolean {
                return oldItem.id == newItem.id &&
                        oldItem.isSelected == newItem.isSelected &&
                        oldItem.text == newItem.text
            }

            override fun areContentsTheSame(oldItem: TextStickerModel, newItem: TextStickerModel): Boolean {
                return oldItem == newItem
            }
        }
    }

}