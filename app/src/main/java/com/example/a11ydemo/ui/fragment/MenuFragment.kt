package com.example.a11ydemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.a11ydemo.databinding.FragmentMenuBinding

/**
 * 줌 대상 컨테이너(menuZoomTarget)를 가진다. Phase 4 ZoomService가
 * scaleX/scaleY를 직접 변경 (M-6 회피, ScaleAnimation 휘발성 대신 영속).
 */
class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            com.example.a11ydemo.accessibility.FocusNavigator
                .findFirstFocusable(view)?.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
