package com.example.universaldownloader.ui.fragments.download

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.universaldownloader.FloatingBubbleService
import com.example.universaldownloader.R
import com.example.universaldownloader.databinding.FragmentDownloadBinding
import com.example.universaldownloader.ui.fragments.viewBinding

class DownloadFragment : Fragment() {

    private lateinit var downloadViewModel: DownloadViewModel
    private val binding: FragmentDownloadBinding by viewBinding(FragmentDownloadBinding::bind)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        downloadViewModel = ViewModelProvider(this).get(DownloadViewModel::class.java)
        return inflater.inflate(R.layout.fragment_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListenerAndObserver()
    }
    private fun setOnClickListenerAndObserver(){
        val textView: TextView = binding.textDownload
        downloadViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        binding.floatingWidgetButton.setOnClickListener {
            activity?.startService(Intent(activity, FloatingBubbleService::class.java)) // TODO: service is broken
            activity?.finish()
        }
    }

}