package com.example.a11ydemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.a11ydemo.R
import com.example.a11ydemo.accessibility.FocusNavigator
import com.example.a11ydemo.databinding.FragmentChecklistBinding
import com.example.a11ydemo.databinding.ItemChecklistRowBinding
import com.example.a11ydemo.prefs.A11yPrefs
import com.example.a11ydemo.service.VolumeService

/**
 * 7기능 체크리스트(CHECK-01). BAR/TTS/HC/FOCUS/MEDIA-Volume/MEDIA-Zoom/TIME.
 * 동적 상태(TTS/HC/Zoom)는 A11yPrefs read로 표시. 정적 항목은 항상 ✅.
 */
class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnChecklistBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnVolumeUp.setOnClickListener { VolumeService.increment() }
        binding.btnVolumeDown.setOnClickListener { VolumeService.decrement() }
        renderRows()
        view.post { FocusNavigator.findFirstFocusable(view)?.requestFocus() }
    }

    override fun onResume() {
        super.onResume()
        renderRows() // 다른 화면에서 토글 후 돌아왔을 때 최신 상태 반영
    }

    private fun renderRows() {
        val container = binding.checklistRows
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        addRow(inflater, container, "✅",
            getString(R.string.checklist_row_bar_label),
            getString(R.string.checklist_row_bar_detail))

        val tts = A11yPrefs.ttsEnabled
        addRow(inflater, container, if (tts) "✅" else "⚠️",
            getString(R.string.checklist_row_tts_label),
            getString(if (tts) R.string.checklist_row_tts_on else R.string.checklist_row_tts_off))

        val hc = A11yPrefs.highContrastEnabled
        addRow(inflater, container, if (hc) "✅" else "⚠️",
            getString(R.string.checklist_row_hc_label),
            getString(if (hc) R.string.checklist_row_hc_on else R.string.checklist_row_hc_off))

        addRow(inflater, container, "✅",
            getString(R.string.checklist_row_focus_label),
            getString(R.string.checklist_row_focus_detail))

        addRow(inflater, container, "✅",
            getString(R.string.checklist_row_volume_label),
            getString(R.string.checklist_row_volume_detail))

        val zoomPct = (A11yPrefs.zoomLevel * 100).toInt()
        addRow(inflater, container, "✅",
            getString(R.string.checklist_row_zoom_label),
            getString(R.string.checklist_row_zoom_detail, zoomPct))

        addRow(inflater, container, "✅",
            getString(R.string.checklist_row_time_label),
            getString(R.string.checklist_row_time_detail))
    }

    private fun addRow(
        inflater: LayoutInflater,
        parent: ViewGroup,
        status: String,
        label: String,
        detail: String
    ) {
        val rowBinding = ItemChecklistRowBinding.inflate(inflater, parent, false)
        rowBinding.rowStatus.text = status
        rowBinding.rowLabel.text = label
        rowBinding.rowDetail.text = detail
        parent.addView(rowBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
