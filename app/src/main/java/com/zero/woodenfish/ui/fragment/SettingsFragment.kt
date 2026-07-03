package com.zero.woodenfish.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.zero.woodenfish.R
import com.zero.woodenfish.WoodenFishViewModel
import com.zero.woodenfish.databinding.FragmentSettingsBinding
import com.zero.woodenfish.model.WoodenFishState
import com.zero.woodenfish.ui.extension.formatAutoTapInterval
import com.zero.woodenfish.ui.extension.formatSecondsValue
import kotlin.math.roundToLong

class SettingsFragment : Fragment() {
    private val viewModel: WoodenFishViewModel by activityViewModels()
    private var binding: FragmentSettingsBinding? = null
    private var syncingControls = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSettingsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        viewModel.state.observe(viewLifecycleOwner) { state ->
            render(state)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupListeners() {
        val currentBinding = binding ?: return
        currentBinding.settingsAutoSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) viewModel.onAutoTapEnabledChanged(checked)
        }
        currentBinding.soundSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) viewModel.onSoundEnabledChanged(checked)
        }
        currentBinding.hapticSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) viewModel.onHapticEnabledChanged(checked)
        }
        currentBinding.immersiveSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) viewModel.onImmersiveEnabledChanged(checked)
        }
        currentBinding.intervalSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.onAutoTapIntervalChanged((value * MILLIS_PER_SECOND).roundToLong())
            }
        }
    }

    private fun render(state: WoodenFishState) {
        val currentBinding = binding ?: return
        currentBinding.settingsAutoSummaryText.text = requireContext().formatAutoTapInterval(state.autoTapIntervalMs)
        currentBinding.intervalValueText.text = getString(
            R.string.current_interval_range_format,
            requireContext().formatSecondsValue(state.autoTapIntervalMs)
        )

        syncingControls = true
        currentBinding.settingsAutoSwitch.isChecked = state.autoTapEnabled
        currentBinding.soundSwitch.isChecked = state.soundEnabled
        currentBinding.hapticSwitch.isChecked = state.hapticEnabled
        currentBinding.immersiveSwitch.isChecked = state.immersiveEnabled
        currentBinding.intervalSlider.value = state.autoTapIntervalMs / MILLIS_PER_SECOND
        syncingControls = false
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000f
    }
}
