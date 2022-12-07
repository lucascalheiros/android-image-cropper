package com.github.lucascalheiros.imagecropper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.github.lucascalheiros.imagecropper.ImageCropperActivity.Companion.EXTRA_RESULT_CROPPED_IMAGE

class ImageCropperContract : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, uri: Uri): Intent {
        return ImageCropperActivity.newIntent(context, uri)
    }

    override fun parseResult(resultCode: Int, result: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return result?.getParcelableExtra(EXTRA_RESULT_CROPPED_IMAGE)
    }
}