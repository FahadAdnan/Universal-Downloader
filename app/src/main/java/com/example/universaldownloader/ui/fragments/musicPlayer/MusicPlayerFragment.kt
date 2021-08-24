package com.example.universaldownloader.ui.fragments.musicPlayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.universaldownloader.R
import com.example.universaldownloader.databinding.FragmentMusicPlayerBinding
import com.example.universaldownloader.ui.fragments.viewBinding

class MusicPlayerFragment : Fragment() {

    private lateinit var dashboardViewModel: MusicPlayerViewModel
    private val binding by viewBinding(FragmentMusicPlayerBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        dashboardViewModel = ViewModelProvider(this).get(MusicPlayerViewModel::class.java)
        return inflater.inflate(R.layout.fragment_music_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = binding.textMusicPlayer
        dashboardViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
    }

}