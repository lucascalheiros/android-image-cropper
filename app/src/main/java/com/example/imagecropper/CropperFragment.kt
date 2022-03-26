package com.example.imagecropper

import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.imagecropper.ImageCropperActivity.Companion.EXTRA_RESULT_CROPPED_IMAGE
import com.example.imagecropper.databinding.FragmentCropperBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CropperFragment : Fragment() {

    private var photoUri: String? = null

    private lateinit var binding: FragmentCropperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoUri = it.getString(ARG_PHOTO_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCropperBinding.inflate(layoutInflater, container, false)

        MainScope().launch {
            val uri = Uri.parse(photoUri)
            val source = withContext(Dispatchers.IO) {
                ImageDecoder.createSource(
                    requireContext().contentResolver,
                    uri
                )
            }
            val bitmap = withContext(Dispatchers.Default) {
                ImageDecoder.decodeBitmap(source)
            }
            binding.cvCropper.photoBitmap = bitmap
        }

        binding.btSaveCrop.setOnClickListener {
            MainScope().launch {
                val bitmap = binding.cvCropper.cropToBitmap() ?: return@launch

                try {
                    val fileName: String = System.currentTimeMillis().toString()
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, PHOTO_DIRECTORY)
                        }
                    }
                    val uri: Uri? = requireContext().contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                    if (uri != null) {
                        withContext(Dispatchers.IO) {
                            requireContext().contentResolver.openOutputStream(uri).use { output ->
                                val bm: Bitmap = bitmap
                                bm.compress(Bitmap.CompressFormat.JPEG, 100, output)
                            }
                        }
                        val intent = Intent()
                        intent.putExtra(EXTRA_RESULT_CROPPED_IMAGE, uri)
                        requireActivity().setResult(RESULT_OK, intent)
                        requireActivity().finish()
                    }
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
        const val ARG_PHOTO_URI = "ARG_PHOTO_URI"

        @JvmStatic
        fun newInstance(photoUri: String) =
            CropperFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_URI, photoUri)
                }
            }
    }
}