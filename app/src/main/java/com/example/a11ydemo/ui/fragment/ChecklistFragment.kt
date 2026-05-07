package com.example.a11ydemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.a11ydemo.accessibility.FocusNavigator
import com.example.a11ydemo.databinding.FragmentChecklistBinding

/**
 * Phase 4에서 7기능(BAR/TTS/HC/FOCUS/MEDIA/TIME) 체크 셀을 채운다.
 * Phase 1은 placeholder + 뒤로가기.
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
        view.post { FocusNavigator.findFirstFocusable(view)?.requestFocus() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
