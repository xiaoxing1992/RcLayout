package app.wei.paging_loader3.adapter

import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.wei.paging_loader3.common.AsyncPageDataDiffer
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 普通加载列表数据的通用 Adapter
 * */
abstract class SimpleDiffAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    protected open val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected open val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<VH>() {

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

    val isEmpty: Boolean
        get() = (itemCount == 0)

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

    open fun append(data: T) = launch { asyncPageDiffer.append(data) }

    open fun append(newList: List<T>) = launch { asyncPageDiffer.append(newList) }

    open fun insert(newList: List<T>, insertPosition: Int) =
        launch { asyncPageDiffer.insert(newList, insertPosition) }

    open fun insert(data: T, insertPosition: Int? = null) =
        launch { asyncPageDiffer.insert(data, insertPosition) }

    open fun reset(newList: List<T>) = launch { asyncPageDiffer.reset(newList) }

    /**
     * 直接主线程更新数据
     * 注意：会忽略内部的更新队列。同步和协程方式混用可能会导致数据混乱
     * */
    fun blockingReset(newList: List<T>) = asyncPageDiffer.blockingReset(newList)

    fun remove(value: T) = launch {
        val list = withContext(workerDispatcher) {
            snapshot().apply { remove(value) }
        }
        changeData(list)
    }

    open fun update(value: T, position: Int, payload: Any? = null) = launch {
        asyncPageDiffer.update(value, position, payload)
    }

    /**
     *  @param values 要更新的<Position,T>
     * */
    open fun update(values: SparseArray<T>, payload: Any? = null) = launch {
        asyncPageDiffer.update(values, payload)
    }

    protected suspend fun changePayload(
        positions: Set<Int>,
        payload: Any?,
        priority: Int = AsyncPageDataDiffer.SingleRunner.DEFAULT_PRIORITY
    ) = asyncPageDiffer.changePayload(positions, payload, priority)

    protected suspend fun changeData(
        newList: List<T>, priority: Int = AsyncPageDataDiffer.SingleRunner.DEFAULT_PRIORITY
    ) {
        asyncPageDiffer.change(newList, priority)
    }
}