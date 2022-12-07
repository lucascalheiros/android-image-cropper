package com.example.imagecropper

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.imagecropper.databinding.ActivitySampleBinding
import com.example.imagecropper.databinding.FragmentSampleBinding
import com.github.lucascalheiros.imagecropper.ImageCropperContract

class SampleFragment : Fragment() {

    lateinit var binding: FragmentSampleBinding

    var onAddButtonPressed = {}

    var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSampleBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            onAddButtonPressed()
        }

        binding.imageView.setImageURI(imageUri)

        return binding.root
    }
}