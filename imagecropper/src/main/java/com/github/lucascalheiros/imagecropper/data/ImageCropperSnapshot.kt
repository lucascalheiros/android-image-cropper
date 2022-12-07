package com.github.lucascalheiros.imagecropper.data

import android.graphics.Matrix

data class ImageCropperSnapshot(
    val photoWidth: Int,
    val photoHeight: Int,
    val matrix: Matrix
)