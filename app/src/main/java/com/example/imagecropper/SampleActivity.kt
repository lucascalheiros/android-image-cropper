package com.example.imagecropper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.imagecropper.databinding.ActivitySampleBinding
import com.github.lucascalheiros.imagecropper.ImageCropperContract

class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            imageResultRegister.launch(0)
        }
    }

    private val imageResultRegister = registerForActivityResult(ImageCropperContract()) { uri ->
        uri ?: return@registerForActivityResult
        binding.imageView.setImageURI(uri)
    }
}