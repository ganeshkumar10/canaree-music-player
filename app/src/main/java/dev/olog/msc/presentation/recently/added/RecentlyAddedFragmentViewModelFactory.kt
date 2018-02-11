package dev.olog.msc.presentation.recently.added

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dev.olog.msc.domain.interactor.detail.recent.GetRecentlyAddedUseCase
import dev.olog.msc.utils.MediaId
import javax.inject.Inject

class RecentlyAddedFragmentViewModelFactory @Inject constructor(
        private val mediaId: MediaId,
        private val useCase: GetRecentlyAddedUseCase

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return RecentlyAddedFragmentViewModel(
                mediaId, useCase
        ) as T
    }
}