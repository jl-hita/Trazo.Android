package com.trazo.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.api.ExportApi
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.diagnostics.launchGuarded
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

data class DataExportState(
    val downloading: Boolean = false,
    /** The file is in memory and a place to save it has to be chosen. */
    val readyToSave: Boolean = false,
    val saved: Boolean = false,
    val saveFailed: Boolean = false,
    val error: ApiError? = null
)

/**
 * Downloads the account's data and writes it where the person chooses. The content is never stored by the app: it is held in
 * memory until it is written or the choice is cancelled.
 */
class DataExportViewModel(private val api: ExportApi, private val io: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _state = MutableStateFlow(DataExportState())
    val state: StateFlow<DataExportState> = _state.asStateFlow()
    private var content: ByteArray? = null

    fun download() {
        if (_state.value.downloading) return
        _state.value = DataExportState(downloading = true)
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.update { it.copy(downloading = false) } }) {
            // The body is read here too: a connection lost half way is a network failure like any other.
            val result = apiCall {
                val response = api.export()
                if (response.isSuccessful) content = withContext(io) { response.body()?.use { it.bytes() } }
                response
            }
            _state.value = when (result) {
                is ApiResult.Success -> DataExportState(readyToSave = content != null)
                is ApiResult.Failure -> DataExportState(error = result.error)
            }
        }
    }

    /** [open] opens the chosen destination; null when the person cancelled, and then the data is dropped. */
    fun save(open: (() -> OutputStream?)?) {
        val bytes = content
        content = null
        if (bytes == null || open == null) {
            _state.value = DataExportState()
            return
        }
        _state.value = DataExportState(downloading = true)
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.value = DataExportState(saveFailed = true) }) {
            val written = withContext(io) {
                try {
                    open()?.use { it.write(bytes); true } ?: false
                } catch (e: IOException) {
                    false
                } catch (e: SecurityException) {
                    Diagnostics.report(e)
                    false
                }
            }
            _state.value = DataExportState(saved = written, saveFailed = !written)
        }
    }
}
