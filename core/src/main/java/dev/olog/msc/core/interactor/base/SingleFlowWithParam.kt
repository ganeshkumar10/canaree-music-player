package dev.olog.msc.core.interactor.base

import dev.olog.msc.core.executors.Dispatcher
import kotlinx.coroutines.withContext

abstract class SingleFlowWithParam<T, in Param>(
    private val schedulers: Dispatcher
) {

    protected abstract suspend fun buildUseCaseObservable(param: Param): T

    suspend fun execute(param: Param): T = withContext(schedulers.worker) {
        buildUseCaseObservable(param)
    }

}