package com.arpadfodor.android.songquiz.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arpadfodor.android.songquiz.model.SpeechRecognizerService
import com.arpadfodor.android.songquiz.model.TextToSpeechService

enum class UserInputState{
    DISABLED, ENABLED, RECORDING
}

enum class TtsState{
    DISABLED, ENABLED, SPEAKING
}

class QuizViewModel : ViewModel() {

    /**
     * User speech input current state
     **/
    val userInputState: MutableLiveData<UserInputState> by lazy {
        MutableLiveData<UserInputState>()
    }

    /**
     * Is text to speech currently speaking
     **/
    val ttsState: MutableLiveData<TtsState> by lazy {
        MutableLiveData<TtsState>()
    }

    /**
     * Number of tts speak button presses
     **/
    val numListening: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    /**
     * Last recognition
     **/
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

        TextToSpeechService.stop()
        SpeechRecognizerService.stopListening()
    }

    fun speakToUser(text: String){

        if(userInputState.value == UserInputState.RECORDING){
            return
        }

        val started = {
            userInputState.postValue(UserInputState.DISABLED)
            ttsState.postValue(TtsState.SPEAKING)
            recognition.postValue("")
        }

        val finished = {
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
            numListening.postValue(numListening.value?.plus(1))
        }

        val error = {
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
        }

        TextToSpeechService.speak(text, started, finished, error)

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
        }

        val error = { errorMessage: String ->
            recognition.postValue(errorMessage)
            userInputState.postValue(UserInputState.ENABLED)
            ttsState.postValue(TtsState.ENABLED)
        }

        SpeechRecognizerService.startListening(started, partial, result, error)

    }

}