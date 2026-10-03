package com.example.projectorremote.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectorremote.databinding.FragmentDebugBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebugViewModel by viewModels()
    private lateinit var adapter: DebugLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DebugLogAdapter(dateFormat)
        binding.recyclerDebug.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDebug.adapter = adapter
        viewModel.logs.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list ?: emptyList())
        }
        binding.clearDebugButton.setOnClickListener { viewModel.clearLogs() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    }
}
