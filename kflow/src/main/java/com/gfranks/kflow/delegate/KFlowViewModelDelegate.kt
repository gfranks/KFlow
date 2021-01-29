package com.gfranks.kflow.delegate

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface KFlowViewModelDelegate<Action, Data, State> {
    val state: MutableLiveData<State>
    val actions: StateFlow<Action?>
    fun dispatch(action: Action)
}

class KFlowViewModelImpl<Action, Data, State> : KFlowViewModelDelegate<Action, Data, State> {
    override val state = MutableLiveData<State>()

    private val _actions = MutableStateFlow<Action?>(null)
    override val actions: StateFlow<Action?>
        get() = _actions

    override fun dispatch(action: Action) {
        _actions.value = action
    }
}