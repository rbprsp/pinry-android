package io.github.relony.pinry.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.relony.pinry.data.PinFilter
import io.github.relony.pinry.data.PinRepository
import io.github.relony.pinry.data.api.Pin
import io.github.relony.pinry.ui.common.Pager

class FeedViewModel(repository: PinRepository, filter: PinFilter) : ViewModel() {
    val pager = Pager(viewModelScope, key = Pin::id) { offset -> repository.page(filter, offset) }

    init {
        pager.loadMore()
    }
}
