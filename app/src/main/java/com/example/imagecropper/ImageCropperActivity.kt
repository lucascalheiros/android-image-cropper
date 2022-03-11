package com.example.imagecropper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.replace

class ImageCropperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropper)
        supportFragmentManager.commit {
            replace<CameraFragment>(R.id.flBaseCropper)
        }
    }
}