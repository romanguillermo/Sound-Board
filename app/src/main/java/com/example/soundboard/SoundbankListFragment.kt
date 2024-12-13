package com.example.soundboard

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoundbankListFragment : Fragment() {

    private val viewModel: SoundViewModel by activityViewModels()
    private lateinit var soundbankRepository: SoundbankRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var soundBankSpinner: Spinner
    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_soundbank_list, container, false)

        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val database = AppDatabase.getDatabase(requireContext())
        soundbankRepository = SoundbankRepository(database.soundbankDao())
        settingsRepository = SettingsRepository(requireContext())

        soundBankSpinner = view.findViewById(R.id.soundBankSpinner)

        // Observe soundbanks and update the Spinner
        lifecycleScope.launch {
            soundbankRepository.allSoundbanks.collect { soundbanks ->
                // Update the spinner
                val soundbankNames = soundbanks.map { it.name }
                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, soundbankNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                soundBankSpinner.adapter = spinnerAdapter

                // Load last selected soundbank and update UI
                val lastSoundbankName = settingsRepository.lastSoundbank.first()
                if (lastSoundbankName.isNotEmpty() && soundbankNames.contains(lastSoundbankName)) {
                    soundBankSpinner.setSelection(soundbankNames.indexOf(lastSoundbankName))
                    val soundbank = soundbanks.firstOrNull { it.name == lastSoundbankName }
                    soundbank?.let {
                        // Load the sounds of the selected soundbank
                        (activity as? MainActivity)?.soundControlsFragment?.loadSoundBank(it)

                        Log.d("SoundbankList", "Loaded last selected soundbank: ${it.name}")
                    }
                }
            }
        }

        soundBankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSoundbankName = parent.getItemAtPosition(position).toString()

                lifecycleScope.launch {
                    val selectedSoundbank = soundbankRepository.allSoundbanks.first().firstOrNull { it.name == selectedSoundbankName }
                    selectedSoundbank?.let {
                        // Load the sounds of the selected soundbank
                        (activity as? MainActivity)?.soundControlsFragment?.loadSoundBank(it)

                        Log.d("SoundbankList", "Loaded soundbank: ${it.name}")

                        // Save last selected soundbank to DataStore
                        settingsRepository.saveLastSoundbank(it.name)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        soundBankSpinner.setOnItemLongClickListener { parent, view, position, id ->
            val selectedSoundbankName = parent.getItemAtPosition(position).toString()
            showDeleteConfirmationDialog(selectedSoundbankName)
            true // Consume the long-click event
        }

        // Set up mute/unmute buttons
        val muteButton: ImageButton = view.findViewById(R.id.muteButton)

        muteButton.setOnClickListener {
            toggleMute(muteButton)
        }

        return view
    }

    private fun showDeleteConfirmationDialog(soundbankName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Soundbank")
            .setMessage("Are you sure you want to delete $soundbankName?")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch {
                    val soundbankToDelete = soundbankRepository.allSoundbanks.first().firstOrNull { it.name == soundbankName }
                    soundbankToDelete?.let {
                        soundbankRepository.delete(it)
                        Log.d("SoundbankList", "Deleted soundbank: ${it.name}")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun toggleMute(muteButton: ImageButton) {
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            // Unmute (set volume to a reasonable level, e.g., half of max volume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 0)
            muteButton.setImageResource(R.drawable.volume) // Change to volume image
        } else {
            // Mute
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            muteButton.setImageResource(R.drawable.mute) // Change to mute image
        }
    }
}