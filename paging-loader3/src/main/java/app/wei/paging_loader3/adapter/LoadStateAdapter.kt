package app.wei.paging_loader3.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.LoadState

abstract class LoadStateAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    var loadState: LoadState = LoadState.NotLoading
        set(loadState) {
            if (field != loadState) {
                val oldItem = displayLoadStateAsItem(field)
                val newItem = displayLoadStateAsItem(loadState)
                if (oldItem && !newItem) {
                    field = loadState
                    notifyItemRemoved(0)
                } else if (newItem && !oldItem) {
                    field = loadState
                    notifyItemInserted(0)
                } else if (oldItem && newItem) {
                    field = loadState
                    notifyItemChanged(0)
                } else {
                    field = loadState
                }
            }
        }

    var retryClickListener: (() -> Unit)? = null

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val viewHolder = onCreateViewHolder(parent, loadState)
        if (loadState is LoadState.Error) {
            viewHolder.itemView.setOnClickListener { retryClickListener?.invoke() }
        }
        return viewHolder
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, loadState)
    }

    abstract fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): VH

    abstract fun onBindViewHolder(holder: VH, loadState: LoadState)

    final override fun getItemViewType(position: Int): Int = getStateViewType(loadState)

    abstract fun getStateViewType(loadState: LoadState): Int

    final override fun getItemCount(): Int = if (displayLoadStateAsItem(loadState)) 1 else 0

    open fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading || loadState is LoadState.Error
    }
}