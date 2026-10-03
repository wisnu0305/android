package com.example.projectorremote.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.projectorremote.R
import com.example.projectorremote.data.Command
import com.example.projectorremote.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private var speechRecognizer: SpeechRecognizer? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            viewModel.setListening(false)
        }
        override fun onError(error: Int) {
            viewModel.setListening(false)
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> getString(R.string.speech_error_audio)
                SpeechRecognizer.ERROR_CLIENT -> getString(R.string.speech_error_client)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.speech_permission_denied)
                SpeechRecognizer.ERROR_NETWORK -> getString(R.string.speech_error_network)
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> getString(R.string.speech_error_network_timeout)
                SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.speech_no_match)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> getString(R.string.speech_error_busy)
                SpeechRecognizer.ERROR_SERVER -> getString(R.string.speech_error_server)
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.speech_error_timeout)
                else -> getString(R.string.speech_not_recognized)
            }
            _binding?.statusText?.text = msg
        }
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.processSpeechAndSend(spoken)
            } else {
                _binding?.statusText?.text = getString(R.string.speech_empty)
            }
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
                setRecognitionListener(recognitionListener)
            }
        }

        binding.micButton.setOnClickListener { startSpeechRecognition() }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            binding.statusText.text = msg
        }
        viewModel.lastSpeechText.observe(viewLifecycleOwner) { text ->
            binding.speechResultText.text = text.ifEmpty { "—" }
        }
        viewModel.lastCommand.observe(viewLifecycleOwner) { cmd ->
            binding.commandResultText.text = if (cmd == Command.UNKNOWN) "—" else cmd.displayName
        }
        viewModel.isListening.observe(viewLifecycleOwner) { listening ->
            binding.micButton.isEnabled = !listening
            binding.micButton.alpha = if (listening) 0.6f else 1f
            binding.statusText.text = if (listening) getString(R.string.listening) else binding.statusText.text
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startListeningInvisible()
        else Toast.makeText(requireContext(), R.string.speech_permission_denied, Toast.LENGTH_SHORT).show()
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.speech_not_supported, Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        startListeningInvisible()
    }

    /**
     * Pengenalan suara tanpa UI sistem (tanpa dialog/overlay Google Assistant).
     * Hanya tampilan aplikasi kita yang terlihat; status "Mendengarkan..." di text kita.
     */
    private fun startListeningInvisible() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        viewModel.setListening(true)
        recognizer.startListening(intent)
    }

    override fun onDestroyView() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _binding = null
        super.onDestroyView()
    }
}
