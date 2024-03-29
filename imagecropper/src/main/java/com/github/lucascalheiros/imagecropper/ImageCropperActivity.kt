package com.github.lucascalheiros.imagecropper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit


class ImageCropperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_cropper)

        val photoUri: Uri = intent?.extras?.get(EXTRA_REQUEST_IMAGE_TO_CROP) as Uri
        supportFragmentManager.commit {
            replace(R.id.flBaseCropper, CropperFragment.newInstance(photoUri))
        }

        supportActionBar?.hide()

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.black)

    }

    companion object {
        const val EXTRA_RESULT_CROPPED_IMAGE = "EXTRA_RESULT_CROPPED_IMAGE"
        private const val EXTRA_REQUEST_IMAGE_TO_CROP = "EXTRA_REQUEST_IMAGE_TO_CROP"

        fun newIntent(context: Context, uri: Uri): Intent {
            return Intent(context, ImageCropperActivity::class.java).apply {
                putExtra(EXTRA_REQUEST_IMAGE_TO_CROP, uri)
            }
        }
    }
}