package com.example.imagecropper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class CropperFragment : Fragment() {
    private var photoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoUri = it.getString(ARG_PHOTO_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cropper, container, false)
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