package com.zero.woodenfish.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.zero.woodenfish.R
import com.zero.woodenfish.WoodenFishViewModel
import com.zero.woodenfish.databinding.FragmentRecordBinding
import com.zero.woodenfish.model.WoodenFishState

class RecordFragment : Fragment() {
    private val viewModel: WoodenFishViewModel by activityViewModels()
    private var binding: FragmentRecordBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentRecordBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.clearTodayButton?.setOnClickListener { viewModel.onClearTodayClicked() }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            render(state)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render(state: WoodenFishState) {
        val currentBinding = binding ?: return
        currentBinding.weekCountText.text = state.weekCount.toString()
        currentBinding.recordTodayText.text = getString(R.string.record_today_format, state.todayCount)
        currentBinding.recordTotalText.text = getString(R.string.record_total_format, state.totalCount)
        currentBinding.recordStreakText.text = getString(R.string.record_streak_format, state.streakDays)
        currentBinding.recordAutoText.text = getString(R.string.record_auto_format, state.todayAutoCount)
    }
}
