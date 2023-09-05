package com.github.lucascalheiros.imagecropper

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.lucascalheiros.imagecropper.ImageCropperActivity.Companion.EXTRA_RESULT_CROPPED_IMAGE
import com.github.lucascalheiros.imagecropper.databinding.FragmentCropperBinding
import com.github.lucascalheiros.imagecropper.utils.BitmapManager
import com.github.lucascalheiros.imagecropper.utils.BitmapManager.Companion.loadBitmap
import kotlinx.coroutines.launch

class CropperFragment : Fragment() {

    private val photoUri: Uri?
        get() = arguments?.getParcelable(ARG_PHOTO_URI)

    private var mBinding: FragmentCropperBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentCropperBinding.inflate(layoutInflater, container, false).also {
            mBinding = it
        }

        loadImage()

        binding.btSaveCrop.setOnClickListener {
            saveCroppedImage()
        }

        binding.btCancel.setOnClickListener {
            requireActivity().finish()
        }

        return binding.root
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    private fun loadImage() {
        lifecycleScope.launch {
            try {
                val bitmap = loadBitmap(photoUri!!)
                mBinding!!.areaCropperView.setBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "::loadImage", e)
                requireActivity().finish()
            }
        }
    }

    private fun saveCroppedImage() {
        lifecycleScope.launch {
            val uri = try {
                val bitmap = mBinding!!.areaCropperView.cropAreaToBitmap()
                val fileName: String = System.currentTimeMillis().toString()
                val bitmapManager = BitmapManager(requireContext())
                bitmapManager.saveBitmap(fileName, bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "::onBtnSavePng", e)
                null
            }
            val intent = Intent()
            intent.putExtra(EXTRA_RESULT_CROPPED_IMAGE, uri)
            requireActivity().setResult(RESULT_OK, intent)
            requireActivity().finish()
        }
    }

    companion object {
        private const val TAG = "CropperFragment"
        private const val ARG_PHOTO_URI = "ARG_PHOTO_URI"

        @JvmStatic
        fun newInstance(photoUri: Uri) =
            CropperFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PHOTO_URI, photoUri)
                }
            }
    }
}