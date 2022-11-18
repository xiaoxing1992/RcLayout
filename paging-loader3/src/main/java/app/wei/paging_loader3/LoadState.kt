package app.wei.paging_loader3

sealed class LoadState {
    /**
     * 初始或者完成状态，不显示 Loading
     * */
    object NotLoading : LoadState()

    /**
     * 有翻页 has more 显示 Loading
     * */
    object Loading : LoadState()

    /**
     * 错误状态，has more 显示 UI，点击重试
     *
     * 可以考虑加入 msg 参数，显示到 UI 上
     * */
    object Error : LoadState()
}