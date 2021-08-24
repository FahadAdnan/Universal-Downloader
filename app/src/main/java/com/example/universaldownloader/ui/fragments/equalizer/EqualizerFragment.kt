package com.example.universaldownloader.ui.fragments.equalizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.universaldownloader.R
import com.example.universaldownloader.databinding.FragmentEqualizerBinding
import com.example.universaldownloader.ui.fragments.viewBinding

class EqualizerFragment : Fragment() {

    private lateinit var notificationsViewModel: EqualizerViewModel
    private val binding by viewBinding(FragmentEqualizerBinding::bind)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        notificationsViewModel = ViewModelProvider(this).get(EqualizerViewModel::class.java)
        return inflater.inflate(R.layout.fragment_equalizer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = binding.textEqualizer
        notificationsViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
    }
}