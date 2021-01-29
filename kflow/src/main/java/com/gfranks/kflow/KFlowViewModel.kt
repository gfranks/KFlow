package com.gfranks.kflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gfranks.kflow.delegate.KFlowViewModelDelegate
import com.gfranks.kflow.delegate.KFlowViewModelImpl
import com.gfranks.kflow.emitter.Emitter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
abstract class KFlowViewModel<Action : Any, Data, State>(
    delegate: KFlowViewModelDelegate<Action, Data, State> = KFlowViewModelImpl()
) : ViewModel(), KFlowViewModelDelegate<Action, Data, State> by delegate {

    private val _state: State
        get() = state.value ?: this.emitter().initialState

    init {
        bindActions()
    }

    abstract fun emitter(): Emitter<Action, Data, State>
    abstract fun bind(flow: Flow<State>): Flow<State>

    private fun bindActions() {
        val flow = actions.filterNotNull()
            .flatMapConcat(this.emitter()::perform)
            .map { this.emitter().emit(it.action, _state, it.data) }

        bind(flow)
            .onEach { state.value = it }
            .launchIn(viewModelScope)
    }

}