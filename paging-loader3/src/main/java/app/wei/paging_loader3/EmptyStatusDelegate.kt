package app.wei.paging_loader3

interface EmptyStatusDelegate {
    fun setStatusListener(statusListener: (emptyStatus: Int, firstAction: Boolean, errorMessage: String?) -> Unit)
}