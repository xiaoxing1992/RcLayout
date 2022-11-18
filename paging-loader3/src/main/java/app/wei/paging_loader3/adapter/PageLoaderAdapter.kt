package app.wei.paging_loader3.adapter

import android.util.Log
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.LoadState
import app.wei.paging_loader3.common.PageComposite
import java.util.concurrent.CopyOnWriteArrayList

/**
 * pageLoader 3 配套组件
 *
 * PageLoader3 使用的 Adapter ，不包含 list 逻辑
 *
 * 推荐使用 diff 处理数据  @see [BasePageDiffAdapter]
 * */
abstract class PageLoaderAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    protected var prefetchOffset = PREFETCH_OFFSET
        set(value) {
            if (value >= 0) {
                field = value
            }
        }

    private val loadStateListeners = CopyOnWriteArrayList<(LoadState) -> Unit>()
    private var loadNextPageListener: (() -> Unit)? = null
    private var loadState: LoadState = LoadState.NotLoading

    init {
        loadStateListeners.add { loadState = it }
    }

    fun setLoadNextPageListener(loadNextPageListener: () -> Unit) {
        this.loadNextPageListener = loadNextPageListener
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            checkPrefetchLoad(position)
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    protected open fun checkPrefetchLoad(position: Int) {
        val loadNextPageListener = loadNextPageListener ?: return
        if (loadState is LoadState.Loading) {
            val offset = itemCount - position - 1
            if (offset <= prefetchOffset) {
                Log.e(
                    "PageRecyclerAdapter3 ", "触发加载下一页 position = $position , itemCount = $itemCount"
                )
                loadNextPageListener.invoke()
            }
        }
    }

    val isEmpty: Boolean
        get() = (itemCount == 0)

    abstract fun insertData(composite: PageComposite<List<T>>, reset: Boolean): Any

    @Synchronized
    protected fun updateLoadState(newState: LoadState) {
        loadStateListeners.forEach { it.invoke(newState) }
    }

    fun showLoadStateError() {
        if (isEmpty) {
            updateLoadState(LoadState.NotLoading)
        } else {
            updateLoadState(LoadState.Error)
        }
    }

    /**
     * 配置 ConcatAdapter.Config
     *
     * 1. 如果 Adapter ViewHolder 只有一个， 使用 ConcatAdapter.Config.DEFAULT
     *
     * 2. 如果 Adapter ViewHolder 有多个，根据 getItemViewType 来区分, 使用 MULTI_TYPE_CONCAT_ADAPTER_CONFIG
     *
     *  说明 ：
     *
     *  需要保证 ConcatAdapter 内所有 getItemViewType 需要唯一。
     *  （getItemViewType 使用 R.layout 的 id 作为索引保证唯一）
     *
     *  ConcatAdapter.Config 需要配置 isolateViewTypes = false


     *  ConcatAdapter.Config.Builder().apply {
     *      setIsolateViewTypes(false)
     *      setStableIdMode(StableIdMode.NO_STABLE_IDS)
     *  }.build()
     *
     * */
    abstract val concatAdapterConfig: ConcatAdapter.Config

    fun withLoadStateFooter(footer: LoadStateAdapter<*>): ConcatAdapter {
        loadStateListeners.add { footer.loadState = it }
        footer.retryClickListener = {
            updateLoadState(LoadState.Loading)
            loadNextPageListener?.invoke()
        }
        return ConcatAdapter(concatAdapterConfig, this, footer)
    }

    companion object {
        private const val PREFETCH_OFFSET = 0

        val MULTI_TYPE_CONCAT_ADAPTER_CONFIG by lazy {
            ConcatAdapter.Config.Builder().apply {
                setIsolateViewTypes(false)
                setStableIdMode(ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS)
            }.build()
        }
    }
}