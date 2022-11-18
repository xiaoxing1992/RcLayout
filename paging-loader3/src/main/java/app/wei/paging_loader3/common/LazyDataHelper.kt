package app.wei.paging_loader3.common

class LazyDataHelper(
    private val firstLoadAction: () -> Unit,
    private val notFirstAction: (() -> Unit)? = null,
) {

    private val state = LazyState()

    /**
     * 执行数据加载
     * 在 onResume 中调用
     * */
    fun lazyFetch() {
        if (state.isLazyFetch) {
            state.isLazyFetch = false
            if (state.noResult) {
                firstLoadAction.invoke()
            } else {
                notFirstAction?.invoke()
            }
        }
    }

    @Deprecated("命名调整", replaceWith = ReplaceWith("received"))
    fun fetchCompleted() {
        state.noResult = false
    }

    /**
     * 请求得到数据结果时（无论成功失败）调用
     *
     * Rxjava 在 doOnEvent 调用
     * 协程 Flow 分别在 catch 和 collect 调用
     * */
    fun received() {
        state.noResult = false
    }

    /**
     * 重置懒加载判断
     * 在 onDestroyView 调用
     * */
    fun reset() {
        state.isLazyFetch = true
    }

    /**
     * 强制标记数据情况，重置懒加载
     *
     * 页面需要重新更换数据时使用
     * 例如页面没有销毁，但页面数据源变更了
     * */
    fun forceReset() {
        state.isLazyFetch = true
        state.noResult = true
    }

    private data class LazyState(var isLazyFetch: Boolean = true, var noResult: Boolean = true)
}