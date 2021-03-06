package com.arpadfodor.android.songquiz.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arpadfodor.android.songquiz.model.ConversationService
import com.arpadfodor.android.songquiz.model.SpeechRecognizerService
import com.arpadfodor.android.songquiz.model.TextToSpeechService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserInputState{
    DISABLED, ENABLED, RECORDING
}

enum class TtsState{
    DISABLED, ENABLED, SPEAKING
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    var conversationService: ConversationService,
    var textToSpeechService: TextToSpeechService,
    var speechRecognizerService: SpeechRecognizerService

) : ViewModel() {

    /**
     * User speech input current state
     */
    val userInputState: MutableLiveData<UserInputState> by lazy {
        MutableLiveData<UserInputState>()
    }

    /**
     * Is text to speech currently speaking
     */
    val ttsState: MutableLiveData<TtsState> by lazy {
        MutableLiveData<TtsState>()
    }

    /**
     * Number of tts speak button presses
     */
    val numListening: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    /**
     * Last info towards the user
     */
    val info: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    /**
     * Last recognition
     */
    val recognition: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    init {
        clearQuizState()
    }

    fun clearQuizState(){
        userInputState.postValue(UserInputState.ENABLED)
        ttsState.postValue(TtsState.ENABLED)
        numListening.postValue(0)
        recognition.postValue("")
        // reset services
        conversationService.reset()
        textToSpeechService.stop()
        speechRecognizerService.stopListening()
    }

    fun speakToUser(clearUserInputText: Boolean = false){

        if(userInputState.value == UserInputState.RECORDING){
            return
        }

        userInputState.postValue(UserInputState.DISABLED)
        ttsState.postValue(TtsState.SPEAKING)

        // get the current info
        val response = conversationService.getCurrentInfo()
        val text = response.first
        val immediateAnswerNeeded = response.second

        val started = {
            info.postValue(text)
            if(clearUserInputText){
                recognition.postValue("")
            }
        }

        val finished = {
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
            numListening.postValue(numListening.value?.plus(1))

            // immediate user response is expected
            if(immediateAnswerNeeded){
                viewModelScope.launch(Dispatchers.Main) {
                    getUserInput()
                }
            }
        }

        val error = {
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
        }

        textToSpeechService.speak(text, started, finished, error)

    }

    fun getUserInput(){

        if(ttsState.value == TtsState.SPEAKING){
            return
        }

        ttsState.postValue(TtsState.DISABLED)
        userInputState.postValue(UserInputState.RECORDING)

        val started = {}

        val partial = { textList: ArrayList<String> ->
            recognition.postValue(textList.toString())
        }

        val result = { textList: ArrayList<String> ->
            recognition.postValue(textList.toString())
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)

            // update state
            val speakToUserNeeded = conversationService.userInput(textList)
            if(speakToUserNeeded){
                viewModelScope.launch(Dispatchers.Main) {
                    speakToUser()
                }
            }
        }

        val error = { errorMessage: String ->
            recognition.postValue(errorMessage)
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
        }

        speechRecognizerService.startListening(started, partial, result, error)

    }

}