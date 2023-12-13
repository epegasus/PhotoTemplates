package dev.pegasus.phototemplates.helpers.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pegasus.phototemplates.R
import dev.pegasus.phototemplates.commons.listeners.OnTemplateItemClickListener
import dev.pegasus.phototemplates.databinding.ItemTemplatesRowLayoutBinding
import dev.pegasus.template.dataClasses.TemplateModel

class TemplatesListAdapter(private val onTemplateItemClickListener: OnTemplateItemClickListener) : ListAdapter<TemplateModel, TemplatesListAdapter.CustomViewHolder>(diffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ItemTemplatesRowLayoutBinding>(layoutInflater, R.layout.item_templates_row_layout, parent, false)
        return CustomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.binding.model = currentItem
        holder.binding.itemClick = onTemplateItemClickListener
    }

    inner class CustomViewHolder(val binding: ItemTemplatesRowLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<TemplateModel>() {
            override fun areItemsTheSame(oldItem: TemplateModel, newItem: TemplateModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TemplateModel, newItem: TemplateModel): Boolean {
                return oldItem == newItem
            }
        }
    }

}