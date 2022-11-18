package app.wei.paging_loader3.adapter

import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.LoadState
import app.wei.paging_loader3.common.PageComposite

/**
 * pageLoader 3 配套组件
 *
 * 新增加的，不再使用了，改用 @see [BasePageDiffAdapter]
 * */
abstract class BasePageAdapter<T, VH : RecyclerView.ViewHolder> : PageLoaderAdapter<T, VH>() {

    protected open val list: MutableList<T> = ArrayList()
    protected open val layoutAnimRes: Int = -1

    // 不开放 override ，方便替换排查旧 adapter.getItemCount 内对 footer +1 的情况
    final override fun getItemCount() = list.size

    @Synchronized
    override fun insertData(composite: PageComposite<List<T>>, reset: Boolean) {
        //if (reset && isSessionProvide) {
        //    trackData.putSession(sessionProvider.newSession())
        //}
        updateLoadState(if (composite.hasMore) LoadState.Loading else LoadState.NotLoading)

        if (reset) {
            list.clear()
            list.addAll(composite.data)
            notifyDataSetChanged()
            // 不能使用 notifyItemRangeChanged 之类的，会导致触发 onBindViewHolder，再导致加载下一页
        } else {
            val newList = composite.data
            if (!newList.isNullOrEmpty()) {
                val newSize = newList.size
                val oldSize = list.size
                list.addAll(newList)
                notifyItemRangeInserted(oldSize, newSize)
            }
        }
    }

    @Synchronized
    open fun remove(value: T) {
        val index: Int = list.indexOf(value)
        if (list.remove(value)) {
            notifyItemRemoved(index)
        }
    }
}