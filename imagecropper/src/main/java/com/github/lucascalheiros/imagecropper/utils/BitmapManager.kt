package com.github.lucascalheiros.imagecropper.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class BitmapManager(private val context: Context) {

    suspend fun saveBitmap(fileName: String, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val contentValues = jpegContentValues(fileName)
        val uri: Uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
        context.contentResolver.openOutputStream(uri).use { output ->
            val bm: Bitmap = bitmap
            bm.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        uri
    }


    companion object {

        suspend fun loadBitmap(uri: Uri) = withContext(Dispatchers.IO) {
            val path = uri.path!!
            BitmapFactory.decodeFile(path).let {
                val exif = ExifInterface(path)
                val orientation: Int =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                val matrix = Matrix()
                when (orientation) {
                    6 -> {
                        matrix.postRotate(90f)
                    }
                    3 -> {
                        matrix.postRotate(180f)
                    }
                    8 -> {
                        matrix.postRotate(270f)
                    }
                }
                Bitmap.createBitmap(
                    it,
                    0,
                    0,
                    it.width,
                    it.height,
                    matrix,
                    true
                )
            }
        }


        fun jpegContentValues(fileName: String): ContentValues {
            return ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, JPEG_MIME)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, PHOTO_DIRECTORY)
                }
            }
        }

        private const val PHOTO_DIRECTORY = "Pictures/ImageCropper"
        private const val JPEG_MIME = "image/jpeg"
    }
}

