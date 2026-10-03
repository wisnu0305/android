package com.example.projectorremote.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.projectorremote.R
import com.example.projectorremote.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadSettings()

        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.profileDropdown.setAdapter(adapter)
        binding.profileDropdown.setOnClickListener { binding.profileDropdown.showDropDown() }

        viewModel.profileOptions.observe(viewLifecycleOwner) { options ->
            val labels = options.map { it.label }
            adapter.clear()
            adapter.addAll(labels)
            adapter.notifyDataSetChanged()
        }

        viewModel.currentIp.observe(viewLifecycleOwner) { ip ->
            binding.ipEditText.setText(ip)
        }

        viewModel.selectedOptionId.observe(viewLifecycleOwner) { optionId ->
            val options = viewModel.profileOptions.value ?: emptyList()
            val index = options.indexOfFirst { it.id == optionId }
            if (index >= 0) {
                binding.profileDropdown.setText(options[index].label, false)
            }
            binding.deleteProfileButton.visibility =
                if (viewModel.canDeleteSelectedProfile()) View.VISIBLE else View.GONE
        }

        viewModel.formProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                binding.profileNameEditText.setText(profile.name)
                binding.customBaudEditText.setText(profile.baud.toString())
                binding.customCmdPonEditText.setText(profile.cmdPon)
                binding.customCmdPoffEditText.setText(profile.cmdPoff)
                binding.customCmdVupEditText.setText(profile.cmdVup)
                binding.customCmdVdownEditText.setText(profile.cmdVdown)
            } else {
                binding.profileNameEditText.text?.clear()
                binding.customBaudEditText.text?.clear()
                binding.customCmdPonEditText.text?.clear()
                binding.customCmdPoffEditText.text?.clear()
                binding.customCmdVupEditText.text?.clear()
                binding.customCmdVdownEditText.text?.clear()
            }
        }

        viewModel.saveMessage.observe(viewLifecycleOwner) { msg ->
            binding.saveStatusText.text = msg
            binding.saveStatusText.visibility = if (msg.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        binding.profileDropdown.setOnItemClickListener { _, _, position, _ ->
            val options = viewModel.profileOptions.value ?: return@setOnItemClickListener
            if (position in options.indices) {
                viewModel.onProfileOptionSelected(options[position].id)
            }
        }

        binding.deleteProfileButton.setOnClickListener {
            val name = viewModel.getSelectedProfileName() ?: ""
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_delete_profile)
                .setMessage(getString(R.string.settings_delete_profile_confirm, name))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.clearSaveMessage()
                    viewModel.deleteSelectedProfile()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.saveButton.setOnClickListener {
            viewModel.clearSaveMessage()
            viewModel.saveSettings(
                ip = binding.ipEditText.text.toString(),
                profileName = binding.profileNameEditText.text.toString(),
                baud = binding.customBaudEditText.text?.toString(),
                cmdPon = binding.customCmdPonEditText.text?.toString(),
                cmdPoff = binding.customCmdPoffEditText.text?.toString(),
                cmdVup = binding.customCmdVupEditText.text?.toString(),
                cmdVdown = binding.customCmdVdownEditText.text?.toString()
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
