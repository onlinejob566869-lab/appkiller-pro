package com.appkiller.pro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.appkiller.pro.R
import com.appkiller.pro.model.AppInfo

class AppListAdapter(
    private val onKillClick: (AppInfo) -> Unit,
    private val onCheckChange: (AppInfo, Boolean) -> Unit,
    private val onWhitelistToggle: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppInfo>()

    fun updateApps(newApps: List<AppInfo>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = apps.size
            override fun getNewListSize() = newApps.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                apps[oldItemPosition].packageName == newApps[newItemPosition].packageName
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                apps[oldItemPosition] == newApps[newItemPosition]
        })
        apps.clear()
        apps.addAll(newApps)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getSelectedApps(): List<AppInfo> = apps.filter { it.isSelected }

    fun selectAll(select: Boolean) {
        apps.forEachIndexed { _, app -> app.isSelected = select }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.appName
        holder.appPackage.text = app.packageName
        holder.appSize.text = app.sizeFormatted
        holder.appIcon.setImageDrawable(app.icon)
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = app.isSelected
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            app.isSelected = isChecked
            onCheckChange(app, isChecked)
        }
        holder.killBtn.setOnClickListener { onKillClick(app) }
        holder.itemView.setOnLongClickListener {
            onWhitelistToggle(app)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.appCheckbox)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val appPackage: TextView = view.findViewById(R.id.appPackage)
        val appSize: TextView = view.findViewById(R.id.appSize)
        val killBtn: ImageButton = view.findViewById(R.id.killBtn)
    }
}
