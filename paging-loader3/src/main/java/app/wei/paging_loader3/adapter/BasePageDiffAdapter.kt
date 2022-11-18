package app.wei.paging_loader3.adapter

import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.LoadState
import app.wei.paging_loader3.common.AsyncPageDataDiffer
import app.wei.paging_loader3.common.PageComposite
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * pageLoader 3 配套组件
 *
 * 加载向下翻页数据的通用 Adapter
 * */
abstract class BasePageDiffAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    protected open val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected open val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : PageLoaderAdapter<T, VH>() {

    /**
     * 除了特殊情况，实现类正常不用访问 asyncPageDiffer
     * */
    protected val asyncPageDiffer by lazy {
        AsyncPageDataDiffer(
            itemCallback = getItemCallback(),
            AdapterListUpdateCallback(this),
            mainDispatcher,
            workerDispatcher
        )
    }

    protected open fun getItemCallback(): DiffUtil.ItemCallback<T> {
        return object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(
                oldItem: T, newItem: T
            ) = (oldItem == newItem)

            override fun areContentsTheSame(
                oldItem: T, newItem: T
            ) = false
        }
    }

    var viewLifecycleScope: CoroutineScope? = null
        private set

    private val currentScope: CoroutineScope
        get() {
            return viewLifecycleScope ?: AsyncPageDataDiffer.DefaultAdapterScope
        }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        viewLifecycleScope = recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope
    }

    private var submitDataId = AtomicInteger(0)

    /**
     *  可操作的最新列表数据
     *  数据甚至有可能界面上还没有显示出来
     *
     *  【数据修改不影响 adapter ，需要 submit{changeData()} 提交更新】
     * */
    fun snapshot(): MutableList<T> = asyncPageDiffer.currentList.toMutableList()

    /**
     *  只读的最新列表数据，暂时只开放 protected 访问
     *  数据甚至有可能界面上还没有显示出来
     * */
    protected fun currentList(): List<T> = asyncPageDiffer.currentList

    final override fun getItemCount() = asyncPageDiffer.itemCount

    protected fun getItem(@IntRange(from = 0) position: Int) = asyncPageDiffer[position]

    fun addUpdatedListener(listener: AsyncPageDataDiffer.UpdatedListener) {
        asyncPageDiffer.addUpdatedListener(listener)
    }

    fun removeUpdatedListener(listener: AsyncPageDataDiffer.UpdatedListener) {
        asyncPageDiffer.removeUpdatedListener(listener)
    }

    final override fun checkPrefetchLoad(position: Int) {
        if (!asyncPageDiffer.inSubmit) {
            super.checkPrefetchLoad(position)
        }
    }

    /**
     * 发送提交数据更新
     * 只接受 suspend 挂起方法
     * */
    protected fun launch(block: suspend () -> Unit): Job {
        val submitId = submitDataId.incrementAndGet()
        return currentScope.launch {
            if (submitDataId.get() == submitId) {
                block()
            }
        }
    }

    override fun insertData(composite: PageComposite<List<T>>, reset: Boolean): Job {
        updateLoadState(if (composite.hasMore) LoadState.Loading else LoadState.NotLoading)
        return launch {
            if (reset) {
                asyncPageDiffer.reset(composite.data)
            } else {
                asyncPageDiffer.append(composite.data)
            }
        }
    }

    fun clear() = launch {
        if (!isEmpty) {
            asyncPageDiffer.reset(emptyList())
        }
    }

    fun remove(value: T) = launch {
        val list = withContext(workerDispatcher) {
            snapshot().apply { remove(value) }
        }
        changeData(list)
    }

    fun update(value: T, position: Int, payload: Any? = null) = launch {
        asyncPageDiffer.update(value, position, payload)
    }

    /**
     *  @param values 要更新的<Position,T>
     * */
    fun update(values: SparseArray<T>, payload: Any? = null) = launch {
        asyncPageDiffer.update(values, payload)
    }

    protected fun changePayload(
        positions: Set<Int>,
        payload: Any?,
        priority: Int = AsyncPageDataDiffer.SingleRunner.DEFAULT_PRIORITY
    ) = launch { asyncPageDiffer.changePayload(positions, payload, priority) }

    protected suspend fun changeData(
        newList: List<T>, priority: Int = AsyncPageDataDiffer.SingleRunner.DEFAULT_PRIORITY
    ) {
        asyncPageDiffer.change(newList, priority)
    }
}