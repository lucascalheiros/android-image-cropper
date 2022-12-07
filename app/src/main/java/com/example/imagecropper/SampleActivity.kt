package com.example.imagecropper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.imagecropper.databinding.ActivitySampleBinding
import com.github.lucascalheiros.imagecropper.ImageCropperContract
import com.github.lucascalheiros.imagecropper.R

class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding

    private val sampleFragment = SampleFragment()

    private val cameraFragment = CameraFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropper)

        sampleFragment.onAddButtonPressed = {
            supportFragmentManager.commit {
                replace(R.id.flBaseCropper, cameraFragment)
            }
        }

        cameraFragment.onPhotoSaved = {
            imageResultRegister.launch(it)
        }

        supportFragmentManager.commit {
            replace(R.id.flBaseCropper, sampleFragment)
        }
    }


    private val imageResultRegister = registerForActivityResult(ImageCropperContract()) { uri ->
        uri ?: return@registerForActivityResult
        sampleFragment.imageUri = uri
        supportFragmentManager.commit {
            replace(R.id.flBaseCropper, sampleFragment)
        }
    }
}