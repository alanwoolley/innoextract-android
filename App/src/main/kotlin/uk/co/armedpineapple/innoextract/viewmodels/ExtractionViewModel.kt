package uk.co.armedpineapple.innoextract.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.co.armedpineapple.innoextract.gogapi.GogApi
import uk.co.armedpineapple.innoextract.gogapi.GogGame
import uk.co.armedpineapple.innoextract.service.InnoValidationResult
import javax.inject.Inject

@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val gogApi: GogApi
) : ViewModel() {

    private val _gogGame = MutableStateFlow<GogGame?>(null)
    val gogGame: StateFlow<GogGame?> = _gogGame.asStateFlow()

    private val _validationResult = MutableStateFlow<InnoValidationResult?>(null)
    val validationResult: StateFlow<InnoValidationResult?> = _validationResult.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _subtitle = MutableStateFlow("")
    val subtitle: StateFlow<String> = _subtitle.asStateFlow()

    private val _target = MutableStateFlow<Uri?>(null)
    val target: StateFlow<Uri?> = _target.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _currentFile = MutableStateFlow("")
    val currentFile: StateFlow<String> = _currentFile.asStateFlow()

    private var gogUpdateJob: Job? = null

    var fileUri: Uri? = null

    fun onFileSelected(uri: Uri) {
        fileUri = uri
    }

    fun onFileValidated(validationResult: InnoValidationResult, uri: Uri) {
        gogUpdateJob?.cancel("Got new validation result.")
        _validationResult.value = validationResult
        if (validationResult.isValid) {
            _title.value = validationResult.title
            _subtitle.value = "Inno Setup " + validationResult.version
            if (validationResult.isGogInstaller) {
                gogUpdateJob = viewModelScope.launch {
                    try {
                        val gogGame = gogApi.getGameDetails((validationResult.gogId))
                        if (isActive) {
                            _gogGame.value = gogGame
                            _title.value = gogGame.title
                            _subtitle.value =
                                validationResult.title + " (Inno Setup " + validationResult.version + ")"
                        }
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Wasn't able to get gog details. ${e.localizedMessage}")
                    }
                }
            }
        } else {
            _gogGame.value = null
            _title.value = ""
            _subtitle.value = ""
        }
    }

    fun updateTarget(uri: Uri) {
        _target.value = uri
    }

    fun updateProgress(progressPct: Int) {
        _progress.value = progressPct.toFloat()
    }

    fun updateStatus(status: String) {
        _status.value = status
    }

    fun onComplete() {
        _isComplete.value = true
    }

    fun onFail() {
        _isError.value = true
    }

    fun reset() {
        _target.value = null
        _status.value = ""
        _progress.value = 0f
        _isComplete.value = false
        _isError.value = false
        _gogGame.value = null
        _title.value = ""
        _subtitle.value = ""
        _validationResult.value = null
        fileUri = null
    }

    companion object {
        private const val LOG_TAG = "ExtractionViewModel"
    }
}
