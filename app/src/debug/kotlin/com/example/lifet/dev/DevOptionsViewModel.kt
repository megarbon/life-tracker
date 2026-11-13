package com.example.lifet.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.demo.DemoDataSeeder
import com.example.lifet.core.demo.DemoScenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface DevStatus {
    data object Idle : DevStatus

    data object Working : DevStatus

    data object Done : DevStatus

    data class Failed(
        val message: String,
    ) : DevStatus
}

internal data class DevOptionsUiState(
    val registeredAreas: Int,
    val status: DevStatus = DevStatus.Idle,
)

@HiltViewModel
internal class DevOptionsViewModel
    @Inject
    constructor(
        private val seeder: DemoDataSeeder,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DevOptionsUiState(seeder.registeredAreas.size))
        val uiState: StateFlow<DevOptionsUiState> = _uiState.asStateFlow()

        fun load(scenario: DemoScenario) = execute { seeder.load(scenario) }

        fun clearAll() = execute { seeder.clearAll() }

        @Suppress("TooGenericExceptionCaught") // Herramienta de depuración: mostrar cualquier fallo.
        private fun execute(block: suspend () -> Unit) {
            // Ignora un segundo toque mientras corre otra operación: dos cargas a la vez duplicarían registros.
            if (_uiState.value.status == DevStatus.Working) return
            _uiState.update { it.copy(status = DevStatus.Working) }
            viewModelScope.launch {
                val result =
                    try {
                        block()
                        DevStatus.Done
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        DevStatus.Failed(e.message ?: e.javaClass.simpleName)
                    }
                _uiState.update { it.copy(status = result) }
            }
        }
    }
