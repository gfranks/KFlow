package com.gfranks.kflow.emitter

import com.gfranks.kflow.data.Output
import kotlinx.coroutines.flow.Flow

interface Emitter<Action, Data, State> {
    val initialState: State
    suspend fun perform(action: Action): Flow<Output<Action, Data>>
    suspend fun emit(action: Action, state: State, data: Data?): State
}