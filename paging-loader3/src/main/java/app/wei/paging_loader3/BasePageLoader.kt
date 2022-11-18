package app.wei.paging_loader3

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.adapter.BasePageDiffAdapter
import app.wei.paging_loader3.adapter.LoadStateAdapter
import app.wei.paging_loader3.adapter.PageLoaderAdapter
import app.wei.paging_loader3.common.PageComposite
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

abstract class BasePageLoader<T> : EmptyStatusDelegate {

    private var internalAdapter: PageLoaderAdapter<T, out RecyclerView.ViewHolder>? = null
    private var concatAdapter: ConcatAdapter? = null

    val isItemEmpty: Boolean
        get() = (concatAdapter?.itemCount ?: 0) <= 0

    abstract var defaultFooterAdapterDelegate: Lazy<LoadStateAdapter<out RecyclerView.ViewHolder>>

    private var listener: PageLoadListener? = null

    var dataStatus: PageDataStatus = PageDataStatus.LOADING
        private set

    private val emptyDelegateData = EmptyDelegateData()

    fun concatAdapter(
        adapter: PageLoaderAdapter<T, out RecyclerView.ViewHolder>,
        footerAdapter: LoadStateAdapter<*>? = null
    ): ConcatAdapter {
        internalAdapter = adapter
        adapter.setLoadNextPageListener { loadNextPage() }
        return adapter.withLoadStateFooter((footerAdapter ?: defaultFooterAdapterDelegate.value))
            .also {
                concatAdapter = it
            }
    }

    /**
     * 页面是否在加载中
     */
    private val uiLoadingState: AtomicBoolean = AtomicBoolean()
    val isLoading: Boolean
        get() = uiLoadingState.get()

    /**
     * 页面能否可以翻页
     */
    private val dataHasMore: AtomicBoolean = AtomicBoolean()
    val hasMore: Boolean
        get() = dataHasMore.get()

    /**
     * 翻页游标
     */
    var cursor: String? = null
        private set

    /**
     * 当前列表是否需要清空
     */
    private var restState: AtomicBoolean = AtomicBoolean()
    val isRest: Boolean
        get() = restState.get()

    fun initPage(
        listener: PageLoadListener,
        initialList: List<T>? = null,
        cursor: String? = null,
        hasMore: Boolean = false
    ) {
        this.listener = listener
        this.restState.set(true)
        this.cursor = cursor
        this.dataHasMore.set(hasMore)

        if (initialList == null) {
            listener.loadPage(cursor)
        } else {
            // 不判断 diff 协程处理
            internalAdapter?.insertData(PageComposite(initialList, cursor, hasMore), true)
            dataStatus = if (isItemEmpty) PageDataStatus.SUCCESS_EMPTY else PageDataStatus.SUCCESS_DATA
            notifyStateChange()
        }
    }

    /**
     * 手动全部重置
     * 这个方法目前开放给搜索，清空状态，同时把 adapter 的数据也清空。
     * */
    fun resetAll() {
        this.restState.set(true)
        this.cursor = null
        this.dataHasMore.set(false)
        // 不判断 diff 协程处理
        internalAdapter?.insertData(PageComposite(emptyList(), null, false), isRest)
        dataStatus = PageDataStatus.LOADING
        notifyStateChange()
    }

    /**
     *  标记 loading 结束
     *  一般在 Rxjava doFinally 调用
     * */
    fun loadCompleted() {
        uiLoadingState.set(false)
    }

    fun loadSuccess(composite: PageComposite<List<T>>, notifyStatus: Boolean = true) {
        val adapter = internalAdapter
        if (adapter is BasePageDiffAdapter) {
            adapter.viewLifecycleScope?.launch {
                adapter.insertData(composite, isRest).join()
                nextCursor(composite.cursor, composite.hasMore)
                if (notifyStatus) {
                    notifyStateChange()
                }
            }
        } else {
            adapter?.insertData(composite, isRest)
            nextCursor(composite.cursor, composite.hasMore)
            if (notifyStatus) {
                notifyStateChange()
            }
        }
    }

    suspend fun loadSuccess(
        process: suspend (BasePageDiffAdapter<*, *>) -> Unit,
        cursor: String?,
        hasMore: Boolean,
        notifyStatus: Boolean = true
    ) {
        val adapter = internalAdapter
        if (adapter is BasePageDiffAdapter) {
            process.invoke(adapter)
            nextCursor(cursor, hasMore)
            if (notifyStatus) {
                notifyStateChange()
            }
        } else {
            throw IllegalStateException("只能接受 BasePageDiffAdapter 类型")
        }
    }

    suspend fun loadSuccess(
        composite: PageComposite<List<T>>,
        insertDataCompletedListener: (suspend () -> Unit),
        notifyStatus: Boolean = true
    ) {
        val adapter = internalAdapter
        if (adapter is BasePageDiffAdapter) {
            adapter.insertData(composite, isRest).join()
            insertDataCompletedListener.invoke()
            nextCursor(composite.cursor, composite.hasMore)
            if (notifyStatus) {
                notifyStateChange()
            }
        } else {
            throw IllegalStateException("只能接受 BasePageDiffAdapter 类型")
        }
    }

    fun loadFailed(errorMessage: String?, notifyStatus: Boolean = true) {
        emptyDelegateData.errorMessage = errorMessage
        dataStatus = if (isItemEmpty) PageDataStatus.ERROR_EMPTY else PageDataStatus.ERROR_DATA
        // 不判断 diff 协程处理
        internalAdapter?.showLoadStateError()
        if (notifyStatus) {
            notifyStateChange()
        }
    }

    private fun updateState(newDataStatus: PageDataStatus) {
        dataStatus = newDataStatus
        notifyStateChange()
    }

    /**
     * 手动更新数据添加状态。 例如：添加评论
     * 只适用于监听 EmptyStatusDelegate 数据变化
     *
     * 需要 adapter 数据真正添加完成后调用
     * */
    fun checkItemAdd() {
        if (!isItemEmpty) {
            updateState(PageDataStatus.SUCCESS_DATA)
        }
    }

    /**
     * 手动更新数据删除状态。例如：删除评论
     * 只适用于监听 EmptyStatusDelegate 数据变化
     *
     * 需要 adapter 数据真正删除完成后调用
     * */
    fun checkItemRemove() {
        if (isItemEmpty) {
            updateState(PageDataStatus.SUCCESS_EMPTY)
        }
    }

    /**
     * 【暂不开放，避免旧页面改动，忘使用 loadSuccess】
     * 手动更新 nextCursor
     * 这个方法开放给，不使用 loadSuccess 进行数据更新的场景使用的。
     * */
    protected fun nextCursor(cursor: String?, hasMore: Boolean) {
        dataStatus = if (isItemEmpty) PageDataStatus.SUCCESS_EMPTY else PageDataStatus.SUCCESS_DATA
        this.cursor = cursor
        this.dataHasMore.set(hasMore)
    }

    /**
     * 返回界面重新加载数据
     */
    fun refreshPage(force: Boolean = false) {
        if (!force && isLoading) {
            return
        }
        restState.set(true)
        uiLoadingState.set(true)
        listener?.loadPage(null)
    }

    /**
     * 加载翻页数据
     */
    fun loadNextPage() {
        if (!uiLoadingState.get() && dataHasMore.get()) {
            restState.set(false)
            uiLoadingState.set(true)
            listener?.loadPage(cursor)
        }
    }

    /**
     * EmptyStatusDelegate 接收 EmptyView statusListener，状态更新
     * */
    final override fun setStatusListener(
        statusListener: (emptyStatus: Int, firstAction: Boolean, errorMessage: String?) -> Unit
    ) {
        emptyDelegateData.statusListener = statusListener
        statusListener.invoke(
            exchangeState(dataStatus), true, emptyDelegateData.errorMessage
        )
    }

    private fun notifyStateChange() {
        emptyDelegateData.statusListener?.invoke(
            exchangeState(dataStatus), false, emptyDelegateData.errorMessage
        )
    }

    protected abstract fun exchangeState(dataStatus: PageDataStatus): Int

    private data class EmptyDelegateData(
        var statusListener: ((emptyStatus: Int, firstAction: Boolean, errorMessage: String?) -> Unit)? = null,
        var errorMessage: String? = null
    )
}