package com.example.soundboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SoundViewModel : ViewModel() {
    private val _currentSoundBank = MutableLiveData<List<Int>>(listOf(418107, 110011, 700380, 527845, 467762))
    val currentSoundBank: LiveData<List<Int>> = _currentSoundBank

    fun updateCurrentSoundBank(newSoundBank: List<Int>) {
        _currentSoundBank.value = newSoundBank
    }
}