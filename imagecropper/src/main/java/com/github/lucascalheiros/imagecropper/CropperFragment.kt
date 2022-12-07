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
import com.github.lucascalheiros.imagecropper.data.CropMode
import kotlinx.coroutines.*


class CropperFragment : Fragment() {

    private val photoUri: Uri?
        get() = arguments?.getParcelable(ARG_PHOTO_URI)

    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCropperBinding.inflate(layoutInflater, container, false)

        MainScope().launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    ImageDecoder.createSource(
                        requireContext().contentResolver,
                        photoUri!!
                    ).let {
                        ImageDecoder.decodeBitmap(it)
                    }
                }
                binding.areaCropperView.setBitmap(bitmap)
            } catch (e: Exception) {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        binding.btSaveCrop.setOnClickListener {
            MainScope().launch {
                try {
                    val bitmap = binding.areaCropperView.cropAreaToBitmap()

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
        fun newInstance(photoUri: Uri) =
            CropperFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PHOTO_URI, photoUri)
                }
            }
    }
}