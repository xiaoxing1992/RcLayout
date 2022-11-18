package app.wei.paging_loader3.common

import android.annotation.SuppressLint
import android.util.SparseArray
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.recyclerview.widget.BatchingListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

/***
 * DiffAdapter 内封装 List 数据的包装类
 */
class AsyncPageDataDiffer<T : Any>(
    @NonNull private val itemCallback: DiffUtil.ItemCallback<T>,
    @NonNull private val listUpdateCallback: ListUpdateCallback,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val pages: PageList<T> = PageList(Collections.synchronizedList(mutableListOf<T>()))
    private var snapshot: List<T>? = null

    val currentList: List<T>
        get() = snapshot ?: pages.list

    operator fun get(@IntRange(from = 0) index: Int): T {
        val snap = snapshot
        if (snap == null) {
            return pages.list[index]
        } else {
            val snapSize = snap.size
            return if (index < snapSize) {
                snap[index]
            } else {
                // 当remove时，并且diff 失败时，由于耗时处理后，ViewHolder更新需要数据，
                // 从snapshot获取不到被删除的index，从pages中提供历史数据避免崩溃
                //Log.i("PageDataAsyncDiffer", "remove ，snapshot里没有数据，page提供index = $index")
                pages.list[index]
            }
        }
    }

    val itemCount: Int
        get() = currentList.size

    val inSubmit: Boolean
        get() = snapshot != null

    private val singleRunner = SingleRunner()

    private val pageDataUpdatedListeners: MutableList<UpdatedListener> = Collections.synchronizedList(
        mutableListOf<UpdatedListener>()
    )

    interface UpdatedListener {
        fun updated(oldSize: Int, newSize: Int)
    }

    fun addUpdatedListener(listener: UpdatedListener) {
        pageDataUpdatedListeners.add(listener)
    }

    fun removeUpdatedListener(listener: UpdatedListener) {
        pageDataUpdatedListeners.remove(listener)
    }

    suspend fun append(newList: List<T>, priority: Int = SingleRunner.DEFAULT_PRIORITY) {
        singleRunner.runInIsolation(priority) {
            if (newList.isEmpty()) {
                return@runInIsolation
            }
            withContext(mainDispatcher) {
                snapshot = null
                val oldSize = pages.list.size
                val appendSize = newList.size
                if (newList.isNotEmpty()) {
                    //Log.e("PageDataAsyncDiffer", "append 完成")
                    pages.list.addAll(newList)
                    listUpdateCallback.onInserted(oldSize, appendSize)
                }
                pageDataUpdatedListeners.forEach { it.updated(oldSize, oldSize + appendSize) }
            }
        }
    }

    suspend fun append(data: T, priority: Int = SingleRunner.DEFAULT_PRIORITY) {
        singleRunner.runInIsolation(priority) {
            withContext(mainDispatcher) {
                snapshot = null
                val oldSize = pages.list.size
                //Log.e("PageDataAsyncDiffer", "append 完成")
                pages.list.add(data)
                listUpdateCallback.onInserted(oldSize, 1)
                pageDataUpdatedListeners.forEach { it.updated(oldSize, oldSize + 1) }
            }
        }
    }

    suspend fun insert(
        newList: List<T>, insertPosition: Int, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            if (newList.isEmpty()) {
                return@runInIsolation
            }
            withContext(mainDispatcher) {
                snapshot = null
                val oldSize = pages.list.size
                val appendSize = newList.size
                if (newList.isNotEmpty()) {
                    pages.list.addAll(insertPosition, newList)
                    listUpdateCallback.onInserted(insertPosition, appendSize)
                }
                pageDataUpdatedListeners.forEach { it.updated(oldSize, oldSize + appendSize) }
            }
        }
    }

    suspend fun insert(
        data: T, insertPosition: Int? = null, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            withContext(mainDispatcher) {
                snapshot = null
                val oldSize = pages.list.size
                if (insertPosition == null || insertPosition >= oldSize) {
                    pages.list.add(data)
                    listUpdateCallback.onInserted(oldSize, 1)
                } else {
                    pages.list.add(insertPosition, data)
                    listUpdateCallback.onInserted(insertPosition, 1)
                }
                pageDataUpdatedListeners.forEach { it.updated(oldSize, oldSize + 1) }
            }
        }
    }

    suspend fun update(
        data: T, position: Int, payload: Any? = null, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            withContext(mainDispatcher) {
                snapshot = null
                val size = pages.list.size
                if (position < size) {
                    pages.list[position] = data
                    listUpdateCallback.onChanged(position, 1, payload)
                }
                pageDataUpdatedListeners.forEach { it.updated(size, size) }
            }
        }
    }

    /**
     *  @param data 要更新的<Position,T>
     * */
    suspend fun update(
        data: SparseArray<T>, payload: Any? = null, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            if (data.isEmpty()) {
                return@runInIsolation
            }
            withContext(mainDispatcher) {
                snapshot = null
                val size = pages.list.size
                data.forEach { position, value ->
                    if (position < size) {
                        pages.list[position] = value
                        listUpdateCallback.onChanged(position, 1, payload)
                    }
                }
                pageDataUpdatedListeners.forEach { it.updated(size, size) }
            }
        }
    }

    @Synchronized
    fun move(
        @IntRange(from = 0) fromPosition: Int, @IntRange(from = 0) toPosition: Int
    ) {
        if (fromPosition < itemCount && toPosition < itemCount && fromPosition != toPosition) {
            val fromValue = pages.list[fromPosition]
            pages.list.removeAt(fromPosition)
            pages.list.add(toPosition, fromValue)
            listUpdateCallback.onMoved(fromPosition, toPosition)
        }
    }

    /**
     * 直接主线程更新数据
     * 注意：暂时开放给外部调用以兼容旧逻辑。由于会忽略内部的更新队列。同步和协程方式混用可能会导致数据混乱
     * */
    fun blockingReset(newList: List<T>) {
        val oldSize = pages.list.size
        val newSize = newList.size
        val changeSize = newSize - oldSize
        if (oldSize == 0 && newSize == 0) {
            // 没有任何数据更新
            snapshot = null
            pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
            return
        }
        snapshot = newList
        pages.list.clear()
        pages.list.addAll(newList)
        snapshot = null
        //Log.e("PageDataAsyncDiffer", "reset 完成")
        when {
            changeSize == 0 -> { // 数量没有变化
                listUpdateCallback.onChanged(0, newSize, null)
            }
            changeSize < 0 -> { // 旧的比新的多，说明删减。
                BatchingListUpdateCallback(listUpdateCallback).run {
                    onRemoved(newSize, abs(changeSize))
                    onChanged(0, newSize, null)
                    dispatchLastEvent()
                }
            }
            changeSize > 0 -> { // 新的比旧的多，说明增加
                BatchingListUpdateCallback(listUpdateCallback).run {
                    onInserted(oldSize, abs(changeSize))
                    onChanged(0, oldSize, null)
                    dispatchLastEvent()
                }
            }
        }
        pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
    }

    suspend fun reset(newList: List<T>, priority: Int = SingleRunner.DEFAULT_PRIORITY) {
        singleRunner.runInIsolation(priority) {
            blockingReset(newList)
        }
    }

    /**
     * 传统更新形式,测试用
     * */
    @SuppressLint("NotifyDataSetChanged")
    suspend fun dataChangedReset(
        newList: List<T>,
        priority: Int = SingleRunner.DEFAULT_PRIORITY,
        adapter: RecyclerView.Adapter<*>
    ) {
        singleRunner.runInIsolation(priority) {
            withContext(mainDispatcher) {
                val oldSize = pages.list.size
                val newSize = newList.size

                snapshot = newList
                pages.list.clear()
                pages.list.addAll(newList)
                snapshot = null
                adapter.notifyDataSetChanged()
                pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
            }
        }
    }

    suspend fun changePayload(
        positions: Set<Int>, payload: Any?, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            if (positions.isEmpty()) {
                return@runInIsolation
            }
            withContext(mainDispatcher) {
                val size = itemCount
                positions.forEach {
                    if (it in 0 until size) { // 检查在 size 范围能
                        listUpdateCallback.onChanged(it, 1, payload)
                    }
                }
                pageDataUpdatedListeners.forEach { it.updated(size, size) }
            }
        }
    }

    suspend fun change(newList: List<T>, priority: Int = SingleRunner.DEFAULT_PRIORITY) {
        submit(newList, priority)
    }

    private suspend fun submit(
        newList: List<T>, priority: Int = SingleRunner.DEFAULT_PRIORITY
    ) {
        singleRunner.runInIsolation(priority) {
            withContext(mainDispatcher) {
                val oldSize = pages.list.size
                val newSize = newList.size
                snapshot = newList
                //Log.i(
                //    "PageDataAsyncDiffer",
                //    "submit 开始处理数据 ${SystemClock.uptimeMillis()} newSize = $newSize, oldSize = $oldSize"
                //)

                // 快速清空
                if (newSize == 0) {
                    pages.list.clear()
                    snapshot = null
                    listUpdateCallback.onRemoved(0, oldSize)
                    pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
                    return@withContext
                }
                // 快速添加
                if (oldSize == 0) {
                    pages.list.addAll(newList)
                    snapshot = null
                    listUpdateCallback.onInserted(0, newSize)
                    pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
                    return@withContext
                }
                // 数据一致下，快速变更（但获取缺少 move 特性）
                if (oldSize == newSize) {
                    if (pages.list == newList) {
                        snapshot = null
                        listUpdateCallback.onChanged(0, newSize, null)
                        pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
                    } else {
                        //虽然快速变更了，但获取缺少 move 特性和动画
                        pages.list.clear()
                        pages.list.addAll(newList)
                        snapshot = null
                        listUpdateCallback.onChanged(0, newSize, null)
                        pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
                    }
                    return@withContext
                }

                var diffResult: DiffUtil.DiffResult? = null
                try {
                    diffResult = withContext(workerDispatcher) {
                        // 异步计算
                        //Log.i("PageDataAsyncDiffer", "submit 准备 异步计算 diffResult")
                        pages.computeDiff(newList, itemCallback)
                    }
                } catch (ignored: Exception) {
                    // 需要用 try 把可能的 cancel 拦截掉。因为最后一定要把 list 数据更新
                }
                if (diffResult != null) {
                    pages.dispatchDiff(listUpdateCallback, newList, diffResult)
                    snapshot = null
                    pageDataUpdatedListeners.forEach { it.updated(oldSize, newSize) }
                    //Log.e("PageDataAsyncDiffer", "submit diff 成功")
                } else {
                    blockingReset(newList)
                    //Log.e("PageDataAsyncDiffer", "submit diff失败，全量刷新")
                }
            }
        }
    }

    companion object {
        val DefaultAdapterScope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = SupervisorJob() + Dispatchers.Main
        }
    }

    //////////////////////////////////
    //////////// PageList ////////////
    //////////////////////////////////
    @JvmInline
    private value class PageList<T>(val list: MutableList<T>)

    private fun <T : Any> PageList<T>.dispatchDiff(
        updateCallback: ListUpdateCallback, newList: List<T>, diffResult: DiffUtil.DiffResult
    ) {
        list.clear()
        list.addAll(newList)
        diffResult.dispatchUpdatesTo(updateCallback)
    }

    private fun <T : Any> PageList<T>.computeDiff(
        newList: List<T>, diffCallback: DiffUtil.ItemCallback<T>
    ): DiffUtil.DiffResult {
        val oldSize = list.size
        val newSize = newList.size
        return DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {

                override fun getOldListSize() = oldSize

                override fun getNewListSize() = newSize

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = list.getOrNull(oldItemPosition)
                    val newItem = newList.getOrNull(newItemPosition)
                    return if (oldItem != null && newItem != null) {
                        diffCallback.areItemsTheSame(oldItem, newItem)
                    } else {
                        oldItem == null && newItem == null
                    }
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    val oldItem = list.getOrNull(oldItemPosition)
                    val newItem = newList.getOrNull(newItemPosition)
                    return if (oldItem != null && newItem != null) {
                        diffCallback.areContentsTheSame(oldItem, newItem)
                    } else {
                        oldItem == null && newItem == null
                    }
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldItem = list.getOrNull(oldItemPosition)
                    val newItem = newList.getOrNull(newItemPosition)
                    return if (oldItem != null && newItem != null) {
                        diffCallback.getChangePayload(oldItem, newItem)
                    } else {
                        null
                    }
                }
            }, true
        )
    }

    /////////////////////////////////////////////////
    //////// SingleRunner ///////////////////////////
    //////// 来自 androidx.paging.SingleRunner////////
    /////////////////////////////////////////////////

    internal class SingleRunner(
        cancelPreviousInEqualPriority: Boolean = true
    ) {
        private val holder = Holder(this, cancelPreviousInEqualPriority)

        suspend fun runInIsolation(
            priority: Int = DEFAULT_PRIORITY, block: suspend () -> Unit
        ) {
            try {
                coroutineScope {
                    val myJob = checkNotNull(coroutineContext[Job]) {
                        "Internal error. coroutineScope should've created a job."
                    }
                    val run = holder.tryEnqueue(
                        priority = priority, job = myJob
                    )
                    if (run) {
                        try {
                            block()
                        } finally {
                            holder.onFinish(myJob)
                        }
                    }
                }
            } catch (cancelIsolatedRunner: CancelIsolatedRunnerException) {
                // if i was cancelled by another caller to this SingleRunner, silently cancel me
                if (cancelIsolatedRunner.runner !== this@SingleRunner) {
                    throw cancelIsolatedRunner
                }
            }
        }

        /**
         * Internal exception which is used to cancel previous instance of an isolated runner.
         * We use this special class so that we can still support regular cancelation coming from the
         * `block` but don't cancel its coroutine just to cancel the block.
         */
        private class CancelIsolatedRunnerException(val runner: SingleRunner) :
            CancellationException()

        private class Holder(
            private val singleRunner: SingleRunner,
            private val cancelPreviousInEqualPriority: Boolean
        ) {
            private val mutex = Mutex()
            private var previous: Job? = null
            private var previousPriority: Int = 0

            suspend fun tryEnqueue(
                priority: Int, job: Job
            ): Boolean {
                mutex.withLock {
                    val prev = previous
                    return if (prev == null || !prev.isActive || previousPriority < priority || (previousPriority == priority && cancelPreviousInEqualPriority)) {
                        prev?.cancel(CancelIsolatedRunnerException(singleRunner))
                        prev?.join()
                        previous = job
                        previousPriority = priority
                        true
                    } else {
                        false
                    }
                }
            }

            suspend fun onFinish(job: Job) {
                mutex.withLock {
                    if (job === previous) {
                        previous = null
                    }
                }
            }
        }

        companion object {
            const val DEFAULT_PRIORITY = 0
        }
    }
}
