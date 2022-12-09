package com.github.lucascalheiros.imagecropper.views

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.github.lucascalheiros.imagecropper.data.CropMode
import com.github.lucascalheiros.imagecropper.databinding.AreaCropperViewBinding


class AreaCropperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(
    context,
    attrs,
    defStyle
) {
    companion object {
        private const val TAG = "AreaCropperView"
    }

    private val binding = AreaCropperViewBinding.inflate(LayoutInflater.from(context), this,  true)

    init {
        setCropMode(CropMode.MoveImage)
        setProportion(16f/9f)
        setHorizontalDefaultCropBorder(200f)
    }

    fun setCropMode(mode: CropMode) {
        when (mode) {
            CropMode.MoveImage -> {
                binding.cropAreaView.allowTouch = false
                binding.cropperView.allowTouch = true
            }
            CropMode.MoveCrop -> {
                binding.cropAreaView.allowTouch = true
                binding.cropperView.allowTouch = false
            }
        }
    }

    fun setProportion(proportion: Float) {
        binding.cropAreaView.cropProportion = proportion
    }

    fun setVerticalDefaultCropBorder(valuePx: Float) {
        binding.cropAreaView.verticalCropBorder = valuePx
    }

    fun setHorizontalDefaultCropBorder(valuePx: Float) {
        binding.cropAreaView.horizontalCropBorder = valuePx
    }

    fun setBitmap(bitmap: Bitmap) {
        binding.cropperView.photoBitmap = bitmap
    }

    suspend fun cropAreaToBitmap(): Bitmap {
        val crop = binding.cropAreaView.takeCropSnapshot()
        return binding.cropperView.cropToBitmap(crop.cropX, crop.cropY, crop.cropWidth, crop.cropHeight)
    }

}

