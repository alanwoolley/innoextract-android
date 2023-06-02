package uk.co.armedpineapple.innoextract.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.co.armedpineapple.innoextract.gogapi.GogGame
import uk.co.armedpineapple.innoextract.gogapi.OkHttpGogApi
import uk.co.armedpineapple.innoextract.service.InnoValidationResult

class ExtractionViewModel(application: Application) : AndroidViewModel(application) {

    private val mutableGogGame: MutableLiveData<GogGame?> = MutableLiveData<GogGame?>()
    private val mutableValidationResult: MutableLiveData<InnoValidationResult?> =
        MutableLiveData<InnoValidationResult?>()
    private val mutableTitle: MutableLiveData<String> = MutableLiveData<String>()
    private val mutableSubtitle: MutableLiveData<String> = MutableLiveData<String>()
    private val mutableTarget: MutableLiveData<Uri?> = MutableLiveData<Uri?>()
    private val mutableProgress: MutableLiveData<Int> = MutableLiveData<Int>()
    private val mutableStatus: MutableLiveData<String> = MutableLiveData<String>()
    private val mutableIsComplete: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    private val mutableIsError: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    private val mutableCurrentFile: MutableLiveData<String> = MutableLiveData<String>()

    private val gogApi = OkHttpGogApi(context = application.applicationContext)
    private var gogUpdateJob: Job? = null

    val gogGame: LiveData<GogGame?> = mutableGogGame
    val validationResult: LiveData<InnoValidationResult?> = mutableValidationResult
    val target: LiveData<Uri?> = mutableTarget
    val title: LiveData<String> = mutableTitle
    val subtitle: LiveData<String> = mutableSubtitle
    val progress: LiveData<Int> = mutableProgress
    val currentFile: LiveData<String> = mutableCurrentFile
    val status: LiveData<String> = mutableStatus
    val isComplete: LiveData<Boolean> = mutableIsComplete
    val isError: LiveData<Boolean> = mutableIsError
    var fileUri: Uri? = null


    fun onFileSelected(uri: Uri) {
        fileUri = uri
    }

    fun onFileValidated(validationResult: InnoValidationResult, uri: Uri) {
        gogUpdateJob?.cancel("Got new validation result.")
        mutableValidationResult.value = validationResult
        if (validationResult.isValid) {
            mutableTitle.value = validationResult.title
            mutableSubtitle.value = "Inno Setup " + validationResult.version
            if (validationResult.isGogInstaller) {
                gogUpdateJob = viewModelScope.launch {
                    val gogGame = gogApi.getGameDetails((validationResult.gogId))
                    if (isActive) {
                        mutableGogGame.value = gogGame
                        mutableTitle.value = gogGame.title
                        mutableSubtitle.value =
                            validationResult.title + " (Inno Setup " + validationResult.version + ")"
                    }
                }
            }
        } else {
            mutableGogGame.value = null
            mutableTitle.value = ""
            mutableSubtitle.value = ""
        }
    }

    fun updateTarget(uri: Uri) {
        mutableTarget.value = uri
    }

    fun updateProgress(progress: Int) {
        mutableProgress.value = progress
    }

    fun updateStatus(status: String) {
        mutableStatus.value = status
    }

    fun onComplete() {
        mutableIsComplete.value = true
    }

    fun onFail() {
        mutableIsError.value = true
    }

    fun reset() {
        mutableTarget.value = null
        mutableProgress.value = 0
        mutableStatus.value = ""
        mutableIsComplete.value = false
        mutableIsError.value = false
        mutableGogGame.value = null
        mutableTitle.value = ""
        mutableSubtitle.value = ""
        mutableValidationResult.value = null
    }
}
