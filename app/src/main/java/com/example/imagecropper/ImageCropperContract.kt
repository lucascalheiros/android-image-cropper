package com.example.imagecropper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.example.imagecropper.ImageCropperActivity.Companion.EXTRA_RESULT_CROPPED_IMAGE

class ImageCropperContract : ActivityResultContract<Int, Uri?>() {
    override fun createIntent(context: Context, code: Int) = Intent(context, ImageCropperActivity::class.java)

    override fun parseResult(resultCode: Int, result: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return result?.getParcelableExtra(EXTRA_RESULT_CROPPED_IMAGE)
    }
}