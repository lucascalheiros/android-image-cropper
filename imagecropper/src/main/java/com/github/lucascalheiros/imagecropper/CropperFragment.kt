package com.github.lucascalheiros.imagecropper

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.lucascalheiros.imagecropper.ImageCropperActivity.Companion.EXTRA_RESULT_CROPPED_IMAGE
import com.github.lucascalheiros.imagecropper.databinding.FragmentCropperBinding
import com.github.lucascalheiros.imagecropper.utils.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CropperFragment : Fragment() {

    private val photoUri: String?
        get() = arguments?.getString(ARG_PHOTO_URI)

    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCropperBinding.inflate(layoutInflater, container, false)

        MainScope().launch {
            val uri = Uri.parse(photoUri)
            val bitmap = withContext(Dispatchers.IO) {
                ImageDecoder.createSource(
                    requireContext().contentResolver,
                    uri
                ).let {
                    ImageDecoder.decodeBitmap(it)
                }
            }
            binding.cvCropper.photoBitmap = bitmap
        }

        binding.btSaveCrop.setOnClickListener {
            MainScope().launch {
                try {
                    val bitmap = binding.cvCropper.cropToBitmap()

                    val fileName: String = System.currentTimeMillis().toString()

                    val fileSaver = FileSaver(requireContext())

                    val uri = fileSaver.saveBitmap(fileName, bitmap)

                    val intent = Intent()
                    intent.putExtra(EXTRA_RESULT_CROPPED_IMAGE, uri)
                    requireActivity().setResult(RESULT_OK, intent)
                    requireActivity().finish()
                } catch (e: Exception) {
                    Log.d(
                        "onBtnSavePng",
                        e.toString()
                    )
                }
            }
        }

        binding.btCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return binding.root
    }

    companion object {
        private const val ARG_PHOTO_URI = "ARG_PHOTO_URI"

        @JvmStatic
        fun newInstance(photoUri: String) =
            CropperFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_URI, photoUri)
                }
            }
    }
}