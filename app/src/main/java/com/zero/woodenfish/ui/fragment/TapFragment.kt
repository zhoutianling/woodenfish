package com.zero.woodenfish.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.zero.woodenfish.R
import com.zero.woodenfish.WoodenFishViewModel
import com.zero.woodenfish.databinding.FragmentTapBinding
import com.zero.woodenfish.model.WoodenFishState
import com.zero.woodenfish.model.WoodenFishUiEvent
import com.zero.woodenfish.ui.extension.formatAutoTapInterval
import com.zero.woodenfish.ui.extension.playMeritFloat
import com.zero.woodenfish.ui.extension.playWoodenFishTapScale

class TapFragment : Fragment() {
    private val viewModel: WoodenFishViewModel by activityViewModels()
    private var binding: FragmentTapBinding? = null
    private var syncingControls = false
    private var lastHandledEventId = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentTapBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lastHandledEventId = viewModel.latestEventId
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupListeners() {
        val currentBinding = binding ?: return
        currentBinding.fishStage.isClickable = true
        currentBinding.fishStage.isFocusable = true
        currentBinding.fishStage.setOnClickListener { viewModel.onManualTap() }
        currentBinding.manualTapButton.setOnClickListener { viewModel.onManualTap() }
        currentBinding.autoSwitch.setOnCheckedChangeListener { _, checked ->
            if (!syncingControls) viewModel.onAutoTapEnabledChanged(checked)
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            render(state)
        }
        viewModel.events.observe(viewLifecycleOwner) { event ->
            handleUiEvent(event)
        }
    }

    private fun render(state: WoodenFishState) {
        val currentBinding = binding ?: return
        val intervalText = requireContext().formatAutoTapInterval(state.autoTapIntervalMs)
        currentBinding.todayCountText.text = state.todayCount.toString()
        currentBinding.autoStatusText.text = if (state.autoTapEnabled) {
            getString(R.string.auto_on)
        } else {
            getString(R.string.auto_off)
        }
        currentBinding.autoIntervalText.text = intervalText

        syncingControls = true
        currentBinding.autoSwitch.isChecked = state.autoTapEnabled
        syncingControls = false
    }

    private fun handleUiEvent(event: WoodenFishUiEvent) {
        if (!isVisible || event.id <= lastHandledEventId) return
        lastHandledEventId = event.id
        when (event) {
            is WoodenFishUiEvent.TapFeedback -> playTapAnimation()
        }
    }

    private fun playTapAnimation() {
        val currentBinding = binding ?: return
        currentBinding.fishImage.playWoodenFishTapScale()
        currentBinding.meritText.playMeritFloat()
        currentBinding.tapEffectView.emitTap()
    }
}
